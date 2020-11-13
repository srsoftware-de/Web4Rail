package de.srsoftware.web4rail;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;

public class PathFinder extends BaseClass{
	private static final Logger LOG = LoggerFactory.getLogger(PathFinder.class);

	private static TreeMap<Integer,List<Route>> availableRoutes(Context context,HashSet<Route> visitedRoutes){
		TreeMap<Integer,List<Route>> availableRoutes = new TreeMap<Integer, List<Route>>();
		
		String inset = "";
		for (int i=0; i<visitedRoutes.size(); i++) inset+="    ";
		
		boolean error = false;
		Block block = context.block;		
		if (isNull(block)) {
			LOG.warn("{} → {}.availableRoutes called without context.block!",inset,Train.class.getSimpleName());
			error = true;
		}
		Train train = context.train;
		if (isNull(train)) {
			LOG.warn("{}→ {}.availableRoutes called without context.train!",inset,Train.class.getSimpleName());
			error = true;
		}
		if (error) return availableRoutes;
		
		Block destination = train.destination();
		Direction direction = context.direction;
/*		if (isSet(direction)) {
			LOG.debug("{}Looking for {}-bound routes from {}",inset,direction,block);
		} else {
			LOG.debug("{}Looking for all routes from {}",inset,block);
		}*/
		if (isSet(destination) && visitedRoutes.isEmpty())	LOG.debug("{}- Destination: {}",inset,destination);

		Route currentRoute = context.route;
		
		for (Route routeCandidate : block.routes()) {
			if (routeCandidate.path().firstElement() != block) continue; // Routen, die nicht vom aktuellen Block starten sind bubu
			if (visitedRoutes.contains(routeCandidate)) {
				LOG.debug("{}→ Candidate {}  would create loop, skipping",inset,routeCandidate.shortName());
				continue;
			}
			if (!routeCandidate.allowed(context)) continue; // Zug darf auf Grund einer nicht erfüllten Bedingung nicht auf die Route
			if (!routeCandidate.isFreeFor(train)) continue; // Route ist nicht frei
			int priority = 0;
			if (isSet(direction) && routeCandidate.startDirection != direction) { // Route startet entgegen der aktuellen Fahrtrichtung des Zuges 
				if (!train.pushPull) continue; // Zug kann nicht wenden
				if (!block.turnAllowed) continue; // Wenden im Block nicht gestattet
				priority -= 5;
			}
			if (routeCandidate == currentRoute) priority-=10; // möglichst andere Route als zuvor wählen // TODO: den Routen einen "last-used" Zeitstempel hinzufügen, und diesen mit in die Priorisierung einbeziehen

			if (isSet(destination)) {
				if (routeCandidate.endBlock() == destination) { // route goes directly to destination
					LOG.debug("{}→ Candidate {} directly leads to {}",inset,routeCandidate.shortName(),destination);
					priority = 1_000_000;
				} else {
					LOG.debug("{}- Candidate: {}",inset,routeCandidate.shortName());
					Context forwardContext = new Context(train);
					forwardContext.block = routeCandidate.endBlock();
					forwardContext.direction = routeCandidate.endDirection.inverse();
					forwardContext.route = null;
					visitedRoutes.add(routeCandidate);
					TreeMap<Integer, List<Route>> forwardRoutes = availableRoutes(forwardContext,visitedRoutes);
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
		if (!availableRoutes.isEmpty())	LOG.debug("{}→ Routes from {}: {}",inset,block,availableRoutes.isEmpty()?"none":"");
		for (Entry<Integer, List<Route>> entry : availableRoutes.entrySet()) {
			LOG.debug("{} - Priority {}:",inset,entry.getKey());
			for (Route r : entry.getValue()) {
				LOG.debug("{}    - {}",inset,r.shortName());
			}
		}
		return availableRoutes;
	}
	
	public static Route chooseRoute(Context context) {
		TreeMap<Integer, List<Route>> availableRoutes = PathFinder.availableRoutes(context,new HashSet<Route>());
		if (availableRoutes.isEmpty()) return null;
		Entry<Integer, List<Route>> entry = availableRoutes.lastEntry();
		List<Route> preferredRoutes = entry.getValue();
		Route selectetRoute = preferredRoutes.get(random.nextInt(preferredRoutes.size())); 
		LOG.debug("Chose \"{}\" with priority {}.",selectetRoute,entry.getKey());
		
		return selectetRoute;
	}

}