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
import de.srsoftware.web4rail.tiles.Tile;

public class RouteManager extends BaseClass {

	public static abstract class Callback {
		public abstract void routePrepared(Route route);
	}

	private static final Logger LOG = LoggerFactory.getLogger(RouteManager.class);
	private Context ctx;
	private Callback callback;
	private boolean searching;

	public RouteManager() {
		searching = false;
	}

	private static TreeMap<Integer, List<Route>> availableRoutes(Context context, HashSet<Route> visitedRoutes) {
		String inset = "";
		for (int i = 0; i < visitedRoutes.size(); i++) inset += "    ";
		LOG.debug("{}{}.availableRoutes({})", inset, RouteManager.class.getSimpleName(), context);

		Block block = context.block();
		Train train = context.train();
		Direction startDirection = context.direction();
		Route currentRoute = context.route();
		TreeMap<Integer, List<Route>> availableRoutes = new TreeMap<Integer, List<Route>>();

		boolean error = false;
		if (isNull(block) && (error = true))
			LOG.warn("{} → {}.availableRoutes called without context.block!", inset, Train.class.getSimpleName());
		if (isNull(train) && (error = true))
			LOG.warn("{}→ {}.availableRoutes called without context.train!", inset, Train.class.getSimpleName());
		if (error) return availableRoutes;

		Block destination = train.destination();
		if (isSet(startDirection)) {
			LOG.debug("{}- Looking for {}-bound routes from {}", inset, startDirection, block);
		} else {
			LOG.debug("{}- Looking for all routes from {}", inset, block);
		}

		if (isSet(destination) && visitedRoutes.isEmpty()) LOG.debug("{}- Destination: {}", inset, destination);

		for (Route routeCandidate : block.leavingRoutes()) {
			if (context.invalidated()) return availableRoutes;
			if (visitedRoutes.contains(routeCandidate)) {
				LOG.debug("{}→ Candidate {}  would create loop, skipping", inset, routeCandidate.shortName());
				continue;
			}

			HashSet<Tile> stuckTrace = train.stuckTrace(); // if train has been stopped in between two blocks lastly:
															// only allow routes that do not conflict with current train
															// position
			if (isSet(stuckTrace) && !routeCandidate.path().containsAll(stuckTrace)) {
				LOG.debug("Stuck train occupies tiles ({}) outside of {} – not allowed.", stuckTrace, routeCandidate);
				continue;
			}

			if (!routeCandidate.allowed(context)) {
				if (routeCandidate.endBlock() != destination) { // allowance may be overridden by destination
					LOG.debug("{} not allowed for {}", routeCandidate, context);
					continue; // Zug darf auf Grund einer nicht erfüllten Bedingung nicht auf die Route
				}
				LOG.debug("{} not allowed for {} – overridden by selected destination", routeCandidate, context);
			}

			int priority = 0;
			if (isSet(startDirection) && routeCandidate.startDirection != startDirection) { // Route startet entgegen
																							// der aktuellen
																							// Fahrtrichtung des Zuges
				if (!train.pushPull) continue; // Zug kann nicht wenden
				if (!block.turnAllowed) continue; // Wenden im Block nicht gestattet
				priority -= 5;
			}
			if (routeCandidate == currentRoute) priority -= 10; // möglichst andere Route als zuvor wählen // TODO: den
																// Routen einen "last-used" Zeitstempel hinzufügen, und
																// diesen mit in die Priorisierung einbeziehen

			if (isSet(destination)) {
				if (routeCandidate.endBlock() == destination) { // route goes directly to destination
					LOG.debug("{}→ Candidate {} directly leads to {}", inset, routeCandidate.shortName(), destination);
					priority = 1_000_000;
				} else {
					LOG.debug("{}- Candidate: {}", inset, routeCandidate.shortName());
					Context forwardContext = new Context(train).block(routeCandidate.endBlock()).route(null)
						.direction(routeCandidate.endDirection);
					visitedRoutes.add(routeCandidate);
					TreeMap<Integer, List<Route>> forwardRoutes = availableRoutes(forwardContext, visitedRoutes);
					visitedRoutes.remove(routeCandidate);
					if (forwardRoutes.isEmpty()) continue; // the candidate does not lead to a block, from which routes
															// to the destination exist
					Entry<Integer, List<Route>> entry = forwardRoutes.lastEntry();
					LOG.debug("{}→ The following routes have connections to {}:", inset, destination);
					for (Route rt : entry.getValue()) LOG.debug("{}  - {}", inset, rt.shortName());
					priority += entry.getKey() - 10;
				}
			}

			List<Route> routeSet = availableRoutes.get(priority);
			if (isNull(routeSet)) {
				routeSet = new Vector<Route>();
				availableRoutes.put(priority, routeSet);
			}
			routeSet.add(routeCandidate);
			if (routeCandidate.endBlock() == destination) break; // direct connection to destination discovered, quit
																	// search
		}
		if (!availableRoutes.isEmpty())
			LOG.debug("{}→ Routes from {}: {}", inset, block, availableRoutes.isEmpty() ? "none" : "");
		for (Entry<Integer, List<Route>> entry : availableRoutes.entrySet()) {
			LOG.debug("{} - Priority {}:", inset, entry.getKey());
			for (Route r : entry.getValue()) LOG.debug("{}    - {}", inset, r.shortName());
		}
		return availableRoutes;
	}

	public static Route chooseRoute(Context context) {
		LOG.debug("{}.chooseRoute({})", RouteManager.class.getSimpleName(), context);
		TreeMap<Integer, List<Route>> availableRoutes = availableRoutes(context, new HashSet<Route>());
		while (!availableRoutes.isEmpty()) {
			if (context.invalidated()) break;
			LOG.debug("availableRoutes: {}", availableRoutes);
			Entry<Integer, List<Route>> entry = availableRoutes.lastEntry();
			List<Route> preferredRoutes = entry.getValue();
			LOG.debug("preferredRoutes: {}", preferredRoutes);
			Route selectedRoute = preferredRoutes.get(random.nextInt(preferredRoutes.size()));
			if (selectedRoute.isFreeFor(context)) {
				LOG.debug("Chose \"{}\" with priority {}.", selectedRoute, entry.getKey());
				return selectedRoute;
			}

			LOG.debug("Selected route \"{}\" is not free for {}", selectedRoute, context);
			preferredRoutes.remove(selectedRoute);
			if (preferredRoutes.isEmpty()) availableRoutes.remove(availableRoutes.lastKey());
		}
		return null;
	}

	public void quit() {
		LOG.debug("{}.quit", this);
		callback = null;
		if (isSet(ctx)) ctx.invalidate();
	}

	public Route prepareRoute(Context context) {
		if (isNull(context) || context.invalidated()) return null;
		Route route = chooseRoute(context);
		if (isNull(route)) return null;
		context.route(route);
		if (!route.reserveFor(context)) {
			route.reset();
			return null;
		}
		if (!route.prepareAndLock()) {
			route.reset();
			return null;
		}
		return route;
	}

	public void setContext(Context context) {
		ctx = context;
	}

	public boolean setCallback(Callback callback) {
		if (ctx.invalidated()) return false;
		this.callback = callback; 
		if (searching) return false; // search already running, do not start again!
		searching = true;		
		Route route = prepareRoute(ctx);
		searching = false;
		if (isNull(route) || isNull(callback)) return false;
		callback.routePrepared(route);
		return true;
	}

	public boolean isSearching() {
		return searching;
	}

	public void start(Callback callback) {
		Thread thread = new Thread() {
			public void run() {
				setCallback(callback);
			};
		};
		thread.setName(getClass().getSimpleName() + "(" + ctx.train() + ")");
		thread.start();
	}
}
