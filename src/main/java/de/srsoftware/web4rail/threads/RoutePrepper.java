package de.srsoftware.web4rail.threads;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.EventListener;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class RoutePrepper extends BaseClass implements Runnable{
	
	

	public static class Trail extends Vector<Route> implements Comparable<Trail> {

		private static final long serialVersionUID = -620911121007236751L;
		private int score = 0;

		@Override
		public int compareTo(Trail other) {
			return other.score() - this.score();
		}
		
		public Trail add(Route route, int score) {
			add(route);
			this.score += score;		
			return this;
		}
		
		public Collection<Trail> derive(Collection<Trail> ongoing) {
			Vector<Trail> derived = new Vector<>();
			for (Trail trail : ongoing) {
				derived.add(trail.prepend(this));
			}
			return derived;
		}
		
		public boolean endsAt(Block block) {
			return lastElement().endBlock() == block;
		}
		
		private Trail prepend(Trail trail) {
			addAll(0, trail);
			score += trail.score;
			return this;
		}
		
		public int score() {
			return score;
		}

		@Override
		public synchronized String toString() {
			StringBuilder sb = new StringBuilder("\n"+getClass().getSimpleName());
			sb.append("(Score ");
			sb.append(score);
			sb.append(": ");
			for (Route r : this) {
				sb.append(r.startBlock().name);
				sb.append(" → ");
			}
			if (!isEmpty())sb.append(lastElement().endBlock().name);
			return sb+")";
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
		
	private static PriorityQueue<Trail> availableRoutes(Context c){
		boolean error = false;
		
		Block startBlock = c.block();
		if (isNull(startBlock) && (error=true)) LOG.warn("RoutePrepper.availableRoutes(…) called without a startBlock!");
		
		Train train = c.train();
		if (isNull(train) && (error=true)) LOG.warn("RoutePrepper.availableRoutes(…) called without a startBlock!");
		
		if (error) return new PriorityQueue<>();
		
		Block destination = train.destination();
		
		Direction startDirection = c.direction();
		LOG.debug("RoutePrepper.availableRoutes({},{},{}), dest = {}",startBlock,startDirection,train,destination);
		
		PriorityQueue<Trail> candidates = routesFrom(c); // map: route → score
		
		if (isNull(destination) || train.isShunting()) {
			LOG.debug("{} has no destination, returning {}",train,candidates);
			return candidates;
		}
		
		LOG.debug("{} is heading for {}, starting breadth-first search…",train,destination);
		
		for (int depth=1; depth<99; depth++) {
			if (candidates.stream().filter(trail -> trail.endsAt(destination)).count() > 0) return candidates; // return connectingTrails + other routes, in case all connecting trails are occupied
			
			PriorityQueue<Trail> nextLevelCandidates = new PriorityQueue<Trail>();
			for (Trail trail : candidates) {
				Route lastRoute = trail.lastElement();
				c.block(lastRoute.endBlock()).direction(lastRoute.endDirection);
				PriorityQueue<Trail> ongoing = routesFrom(c);
				if (!ongoing.isEmpty()) nextLevelCandidates.addAll(trail.derive(ongoing));
			}
			candidates = nextLevelCandidates;
		
		}
		return new PriorityQueue<>();
		
	}
	
	private static Route chooseRoute(Context context) {
		LOG.debug("{}.chooseRoute({})", RoutePrepper.class.getSimpleName(), context);
		PriorityQueue<Trail> availableRoutes = availableRoutes(context);
		LOG.debug("available routes: {}",availableRoutes);
		while (!availableRoutes.isEmpty()) {
			if (context.invalidated()) break;
			LOG.debug("availableRoutes: {}", availableRoutes);
			
			Vector<Route> preferredRoutes = new Vector<>();
			Integer score = null;
			while (!availableRoutes.isEmpty()) {
				Trail trail = availableRoutes.peek();
				if (score == null) score = trail.score();
				if (score != trail.score()) break;
				availableRoutes.remove(trail);
				preferredRoutes.add(trail.firstElement());
			}

			LOG.debug("preferredRoutes: {}", preferredRoutes);

			while (!preferredRoutes.isEmpty()) {
				Route selectedRoute = preferredRoutes.remove(random.nextInt(preferredRoutes.size()));
				HashSet<Tile> stuckTrace = context.train().stuckTrace(); // if train has been stopped in between two blocks lastly: 
	                                                                     // only allow starting routes that do not conflict with current train position
				if (isSet(stuckTrace) && !selectedRoute.path().containsAll(stuckTrace)) {
					LOG.debug("Stuck train occupies tiles ({}) outside of {} – not allowed.", stuckTrace, selectedRoute);
					continue;
				} else if (!selectedRoute.isFreeFor(context)) {
					LOG.debug("Selected route \"{}\" is not free for {}", selectedRoute, context);
					continue;
				}
				LOG.debug("Chose \"{}\" with priority {}.", selectedRoute, score);
				return selectedRoute;				
			}
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

	private static PriorityQueue<Trail> routesFrom(Context context){
		boolean error = false;
		
		Block startBlock = context.block();
		if (isNull(startBlock) && (error=true)) LOG.warn("RoutePrepper.routesFrom(…) called without a startBlock!");
		
		Train train = context.train();
		if (isNull(train) && (error=true)) LOG.warn("RoutePrepper.routesFrom(…) called without a startBlock!");
		
		if (error) return null;
		
		Block destination = train.destination();
		
		Direction startDirection = context.direction();
		
		LOG.debug("     RoutePrepper.routesFrom({},{},{}), dest = {}:",startBlock,startDirection,train,destination);
			
		PriorityQueue<Trail> trails = new PriorityQueue<>();
		
		for (Route route : startBlock.leavingRoutes()) {
			int score = (route.endBlock() == destination) ? 100_000 : 0;
			
			if (isSet(startDirection) && route.startDirection != startDirection) { // Route startet entgegen der aktuellen Fahrtrichtung des Zuges
				if (!train.pushPull) continue; // Zug kann nicht wenden
				if (!startBlock.turnAllowed) continue; // Wenden im Block nicht gestattet
				score -= 5;
			}
			
			LOG.debug("     - evaluating {}",route);
			
			if (!route.allowed(new Context(train).block(startBlock).direction(startDirection))) {
				LOG.debug("       - {} not allowed for {}", route, train);
				if (route.endBlock() != destination) continue;
				LOG.debug("           …overridden by destination of train!", route, train);
			}
			
			trails.add(new Trail().add(route,score));
		}
		
		return trails;
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
