package de.srsoftware.web4rail.threads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.EventListener;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class RoutePrepper extends BaseClass implements Runnable{
	
	private static class Candidate{

		private int score;
		private Route route;

		public Candidate(Route r, int s) {
			route = r;
			score = s;
		}
		
		@Override
		public String toString() {
			return route.toString().replace(")", ", score: "+score+")");
		}
	}
	
	private Context context;
	private Route route;
	private List<EventListener> failListeners = new LinkedList<>(); 
	private List<EventListener> foundListeners = new LinkedList<>();
	private List<EventListener> lockedListeners = new LinkedList<>();
	private EventListener preparedListener = null;
	
	public RoutePrepper(Context c) {
		LOG.debug("new RoutePrepper({})",c);
		List<String> errors = new LinkedList<>();
		if (isNull(c.train())) errors.add(t("No train in context for {}",getClass().getSimpleName()));
		if (isNull(c.block())) errors.add(t("No block in context for {}",getClass().getSimpleName()));
		if (isNull(c.direction())) errors.add(t("No direction in context for {}",getClass().getSimpleName()));
		if (!errors.isEmpty()) throw new NullPointerException(String.join(", ", errors));
		context = c;
	}
		
	private static TreeMap<Integer,LinkedList<Route>> availableRoutes(Context c){
		boolean error = false;
		
		Block startBlock = c.block();
		if (isNull(startBlock) && (error=true)) LOG.warn("RoutePrepper.availableRoutes(…) called without a startBlock!");
		
		Train train = c.train();
		if (isNull(train) && (error=true)) LOG.warn("RoutePrepper.availableRoutes(…) called without a startBlock!");
		
		if (error) return new TreeMap<>();
		
		Block destination = train.destination();
		
		Direction startDirection = c.direction();
		LOG.debug("RoutePrepper.availableRoutes({},{},{}), dest = {}",startBlock,startDirection,train,destination);
		
		TreeMap<Integer, LinkedList<Route>> candidates = routesFrom(c);
		
		if (isNull(destination)) {
			LOG.debug("{} has no destination, returning {}",train,candidates);
			return candidates;
		}
		
		LOG.debug("{} is heading for {}, starting breadth-first search…",train,destination);
		
		HashMap<Route,Candidate> predecessors = new HashMap<>() {
			private static final long serialVersionUID = -42682947866294566L;

			public String toString() {
				return entrySet().stream()
					.sorted((e1,e2) -> e1.getValue().toString().compareTo(e2.getValue().toString()))
					.map(entry -> entry.getValue()+" → "+entry.getKey())
					.collect(Collectors.joining("\n"));
			};
		};
		candidates.entrySet().stream().flatMap(entry -> entry.getValue().stream()).forEach(route -> predecessors.put(route, null));
		TreeMap<Integer,LinkedList<Route>> routesToDest = new TreeMap<>();
		
		int level = 0;
		
		while (!candidates.isEmpty()) {
			LOG.debug("Candidates for level {}:",level);
			candidates.entrySet().stream().flatMap(entry -> entry.getValue().stream()).forEach(route -> LOG.debug(" - {}",route));

			TreeMap<Integer, LinkedList<Route>> queue = new TreeMap<>();
			
			while (!candidates.isEmpty()) {
				Candidate candidate = pop(candidates);
				LOG.debug(" - examining {}…",candidate);
				
				Block endBlock = candidate.route.endBlock();
				Direction endDir = candidate.route.endDirection;
				
				if (endBlock == destination) {
					LOG.debug(" - {} reaches destination!",candidate);
					int score = candidate.score;
					
					// The route we found leads to the destination block.
					// However it might be the last route in a long path.
					// Thus, we need to get the first route in this path:					
					while (predecessors.containsKey(candidate.route)) { 
						Candidate predecessor = predecessors.get(candidate.route);
						if (isNull(predecessor)) break;
						LOG.debug("   - {} is predecessed by {}",candidate,predecessor);
						candidate = predecessor;
						score += candidate.score;
					}
					
					LOG.debug(" → path starts with {} and has total score of {}",candidate.route,score);
					LinkedList<Route> routesForScore = routesToDest.get(score);
					if (isNull(routesForScore)) routesToDest.put(score, routesForScore = new LinkedList<Route>());
					routesForScore.add(candidate.route);
					continue;
				}
				
				LOG.debug("   - {} not reaching {}, adding ongoing routes to queue:",candidate,destination);
				TreeMap<Integer, LinkedList<Route>> successors = routesFrom(c.clone().block(endBlock).direction(endDir));
				while (!successors.isEmpty()) {
					int score = successors.firstKey();
					LinkedList<Route> best = successors.remove(score);
					score -= 25; // Nachfolgeroute
					for (Route route : best) {
						LOG.debug("     - queueing {} with score {}",route,score);
						if (predecessors.containsKey(route)) {
							LOG.debug("this route already has a predecessor: {}",predecessors.get(route));
							continue; // Route wurde bereits besucht
						}
						predecessors.put(route, candidate);
						
						LinkedList<Route> list = queue.get(score);
						if (isNull(list)) queue.put(score, list = new LinkedList<>());
						list.add(route);
					}
				}
			}
			
			if (!routesToDest.isEmpty()) return routesToDest;
			LOG.debug("No routes to {} found with distance {}!",destination,level);
			level ++;
			candidates = queue;			
		}
		LOG.debug("No more candidates for routes towards {}!",destination);
		
		return routesToDest;
		
	}
	
	private static Route chooseRoute(Context context) {
		LOG.debug("{}.chooseRoute({})", RoutePrepper.class.getSimpleName(), context);
		TreeMap<Integer, LinkedList<Route>> availableRoutes = availableRoutes(context);
		LOG.debug("available routes: {}",availableRoutes);
		while (!availableRoutes.isEmpty()) {
			if (context.invalidated()) break;
			LOG.debug("availableRoutes: {}", availableRoutes);
			Entry<Integer, LinkedList<Route>> entry = availableRoutes.lastEntry();
			List<Route> preferredRoutes = entry.getValue();
			LOG.debug("preferredRoutes: {}", preferredRoutes);
			Route selectedRoute = preferredRoutes.get(random.nextInt(preferredRoutes.size()));
			
			HashSet<Tile> stuckTrace = context.train().stuckTrace(); // if train has been stopped in between two blocks lastly: 
                                                                     // only allow starting routes that do not conflict with current train position
			if (isSet(stuckTrace) && !selectedRoute.path().containsAll(stuckTrace)) {
				LOG.debug("Stuck train occupies tiles ({}) outside of {} – not allowed.", stuckTrace, selectedRoute);
			} else if (selectedRoute.isFreeFor(context)) {
				LOG.debug("Chose \"{}\" with priority {}.", selectedRoute, entry.getKey());
				return selectedRoute;
			}

			LOG.debug("Selected route \"{}\" is not free for {}", selectedRoute, context);
			preferredRoutes.remove(selectedRoute);
			if (preferredRoutes.isEmpty()) availableRoutes.remove(availableRoutes.lastKey());
		}
		return null;
	}
	
	private boolean fail() {
		notify(failListeners);
		route = null;
		return false;
	}	
	
	private void notify(List<EventListener> listeners) {
		for (EventListener listener: listeners) {
			listener.fire();
		}		
	}
	
	public void onFail(EventListener l) {
		failListeners.add(l);
	}
	
	public void onRouteFound(EventListener l) {
		foundListeners.add(l);
	}
	
	public void onRouteLocked(EventListener l) {
		lockedListeners.add(l);
	}
	
	public void onRoutePrepared(EventListener l) {
		preparedListener = l;
	}
	
	private static Candidate pop(TreeMap<Integer, LinkedList<Route>> candidates) {
		while (!candidates.isEmpty()) {
			int score = candidates.firstKey();
			LinkedList<Route> list = candidates.get(score);
			if (isNull(list) || list.isEmpty()) {
				candidates.remove(score);
			} else {
				Candidate candidate = new Candidate(list.removeFirst(),score);
				if (list.isEmpty()) candidates.remove(score);
				return candidate;
			}
		}
		return null;
	}
	
	public boolean prepareRoute() {
		if (isNull(context) || context.invalidated()) return fail();
		route = chooseRoute(context);		
		if (isNull(route)) return fail();			
		context.route(route);
		notify(foundListeners);
		if (!route.reserveFor(context)) return fail();
		notify(lockedListeners);
		if (!route.prepareAndLock()) return fail();
		if (isSet(preparedListener)) preparedListener.fire();
		return true;
	}


	public Route route() {
		return route;
	}

	private static TreeMap<Integer,LinkedList<Route>> routesFrom(Context c){
		boolean error = false;
		
		Block startBlock = c.block();
		if (isNull(startBlock) && (error=true)) LOG.warn("RoutePrepper.routesFrom(…) called without a startBlock!");
		
		Train train = c.train();
		if (isNull(train) && (error=true)) LOG.warn("RoutePrepper.routesFrom(…) called without a startBlock!");
		
		if (error) return null;
		
		Block destination = train.destination();
		
		Direction startDirection = c.direction();
		
		LOG.debug("     RoutePrepper.routesFrom({},{},{}), dest = {}",startBlock,startDirection,train,destination);
			
		TreeMap<Integer, LinkedList<Route>> routes = new TreeMap<>();
		
		for (Route route : startBlock.leavingRoutes()) {
			LOG.debug("     - evaluating {}",route);

			int score = 0;
			
			if (!route.allowed(new Context(train).block(startBlock).direction(startDirection))) {
				LOG.debug("       - {} not allowed for {}", route, train);
				if (route.endBlock() != destination) continue;
				LOG.debug("           …overridden by destination of train!", route, train);
			}
			
			if (route.endBlock() == destination) score = 100_000;
			
			if (isSet(startDirection) && route.startDirection != startDirection) { // Route startet entgegen der aktuellen Fahrtrichtung des Zuges
				if (!train.pushPull) continue; // Zug kann nicht wenden
				if (!startBlock.turnAllowed) continue; // Wenden im Block nicht gestattet
				score -= 5;
			}
			
			LinkedList<Route> routesForScore = routes.get(score);
			if (isNull(routesForScore)) routes.put(score, routesForScore = new LinkedList<Route>());
			LOG.debug("       → candidate!");
			routesForScore.add(route);
		}
		
		return routes;
	}
	
	@Override
	public void run() {
		LOG.debug("{}.run()",this);
		prepareRoute();
	}

	public void start() {
		new Thread(this,Application.threadName(this)).start();
	}

	public void stop() {
		context.invalidate();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+context+")";
	}
}
