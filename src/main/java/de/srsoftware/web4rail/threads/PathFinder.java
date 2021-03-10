package de.srsoftware.web4rail.threads;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;

/**
 * @author Stephan Richter, SRSoftware 2020-2021 
 */
public abstract class PathFinder extends BaseClass implements Runnable, Train.Listener{
	public static final Logger LOG = LoggerFactory.getLogger(PathFinder.class);
//	private Context context;
	private boolean aborted = false;
	private Direction direction;
	private Block startBlock;
	private Train train;
	
	public PathFinder(Train train, Block start, Direction direction) {
		this.train = train;		
		this.startBlock = start;
		this.direction = direction;
	}
	
	public void abort() {
		aborted = true;
		aborted();
		LOG.debug("aborted {}",this);
	}

	private static TreeMap<Integer,List<Route>> availableRoutes(Train train, Block start, Direction startDir, HashSet<Route> visitedRoutes){
		String inset = "";
		for (int i=0; i<visitedRoutes.size(); i++) inset+="    ";

		LOG.debug(inset+"PathFinder.availableRoutes({})",visitedRoutes);
		
		boolean error = false;
		if (isNull(start)) {
			LOG.warn("{} → {}.availableRoutes called without start block!",inset,Train.class.getSimpleName());
			error = true;
		}
		if (isNull(train)) {
			LOG.warn("{}→ {}.availableRoutes called without train!",inset,Train.class.getSimpleName());
			error = true;
		}
		if (error) return new TreeMap<Integer, List<Route>>();
		
		if (isSet(startDir)) {
			LOG.debug("{}Looking for {}-bound routes from {}",inset,startDir,start);
		} else {
			LOG.debug("{}Looking for all routes from {}",inset,start);
		}//*/
		Block destination = train.destination();
		if (isSet(destination) && visitedRoutes.isEmpty())	LOG.debug("{}- Destination: {}",inset,destination);

		//Route currentRoute = context.route();
		TreeMap<Integer,List<Route>> availableRoutes = new TreeMap<Integer, List<Route>>();
				
		for (Route routeCandidate : start.routes()) {
			if (routeCandidate.path().firstElement() != start) continue; // Routen, die nicht vom aktuellen Block starten sind bubu
			if (visitedRoutes.contains(routeCandidate)) {
				LOG.debug("{}→ Candidate {}  would create loop, skipping",inset,routeCandidate.shortName());
				continue;
			}
			Context c = new Context(train).block(start).direction(startDir);
			if (!routeCandidate.allowed(c)) { 
				if (routeCandidate.endBlock() != destination) { // allowance may be overridden by destination
					LOG.debug("{} not allowed for {}",routeCandidate,c);
					continue; // Zug darf auf Grund einer nicht erfüllten Bedingung nicht auf die Route
				}
				LOG.debug("{} not allowed for {} – overridden by selected destination",routeCandidate,c);
			}

			int priority = 0;
			if (isSet(startDir) && routeCandidate.startDirection != startDir) { // Route startet entgegen der aktuellen Fahrtrichtung des Zuges 
				if (!train.pushPull) continue; // Zug kann nicht wenden
				if (!start.turnAllowed) continue; // Wenden im Block nicht gestattet
				priority -= 5;
			}
			//if (routeCandidate == currentRoute) priority-=10; // möglichst andere Route als zuvor wählen // TODO: den Routen einen "last-used" Zeitstempel hinzufügen, und diesen mit in die Priorisierung einbeziehen

			if (isSet(destination)) {
				if (routeCandidate.endBlock() == destination) { // route goes directly to destination
					LOG.debug("{}→ Candidate {} directly leads to {}",inset,routeCandidate.shortName(),destination);
					priority = 1_000_000;
				} else {
					LOG.debug("{}- Candidate: {}",inset,routeCandidate.shortName());
					visitedRoutes.add(routeCandidate);
					TreeMap<Integer, List<Route>> forwardRoutes = availableRoutes(train, routeCandidate.endBlock(), routeCandidate.endDirection, visitedRoutes);
					visitedRoutes.remove(routeCandidate);
					if (forwardRoutes.isEmpty()) continue; // the candidate does not lead to a block, from which routes to the destination exist
					Entry<Integer, List<Route>> entry = forwardRoutes.lastEntry();
					LOG.debug("{}→ The following routes have connections to {}:",inset,destination);
					for (Route rt: entry.getValue()) LOG.debug("{}  - {}",inset,rt.shortName());
					priority += entry.getKey()-10;
				}
			}
			
			List<Route> routeSet = availableRoutes.get(priority);
			if (isNull(routeSet)) {
				routeSet = new Vector<Route>();
				availableRoutes.put(priority, routeSet);
			}
			routeSet.add(routeCandidate);
			if (routeCandidate.endBlock() == destination) break; // direct connection to destination discovered, quit search
		}
		if (!availableRoutes.isEmpty())	LOG.debug("{}→ Routes from {}: {}",inset,start,availableRoutes.isEmpty()?"none":"");
		for (Entry<Integer, List<Route>> entry : availableRoutes.entrySet()) {
			LOG.debug("{} - Priority {}:",inset,entry.getKey());
			for (Route r : entry.getValue()) {
				LOG.debug("{}    - {}",inset,r.shortName());
			}
		}
		return availableRoutes;
	}
	
	public Route chooseRoute() {
		LOG.debug("PathFinder.chooseRoute()");
		HashSet<Route> visitedRoutes = new HashSet<Route>();
		TreeMap<Integer, List<Route>> availableRoutes = availableRoutes(train, startBlock, direction,visitedRoutes);
		while (!availableRoutes.isEmpty()) {
			LOG.debug("availableRoutes: {}",availableRoutes);
			Entry<Integer, List<Route>> entry = availableRoutes.lastEntry();
			List<Route> preferredRoutes = entry.getValue();
			LOG.debug("preferredRoutes: {}",preferredRoutes);
			Route selectedRoute = preferredRoutes.get(random.nextInt(preferredRoutes.size()));
			if (selectedRoute.isFreeFor(train)) {
				LOG.debug("Chose \"{}\" with priority {}.",selectedRoute,entry.getKey());
				return selectedRoute;
			}
		
			LOG.debug("Selected route \"{}\" is not free for {}",selectedRoute,train);
			preferredRoutes.remove(selectedRoute);
			if (preferredRoutes.isEmpty()) availableRoutes.remove(availableRoutes.lastKey());
		}
		return null;
	}

	@Override
	public void run() {		
		while (true) {
			if (aborted) return;
			Route route = chooseRoute();
			if (isSet(route)) {
				found(route);
				if (aborted) return;
				if (route.allocateFor(train)) {
					locked(route);
					if (aborted) return;
					if (route.prepareFor(train)) {
						prepared(route);
						return;
					}
				}
			}
			sleep(1000);
		}
	}
	
	public abstract void aborted();
	public abstract void locked(Route r);
	public abstract void found(Route r);
	public abstract void prepared(Route r);
	
	@Override
	public void on(Signal signal) {
		switch (signal) {
		case STOP:
			abort();
			break;
		}
	}
	
	public void start() {
		train.addListener(this);
		Thread thread = new Thread(this);
		thread.setName("Pathfinder("+train+")");
		thread.start();
	}
}
