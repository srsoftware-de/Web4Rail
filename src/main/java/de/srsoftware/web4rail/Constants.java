package de.srsoftware.web4rail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * this interface collects constants inherited to other classes of this application
 * 
 * @author Stephan Richter, SRSoftware
 *
 */
public interface Constants {
	public static final String ACTION = "action";
	public static final String ACTION_ADD = "add";
	public static final String ACTION_ADD_ACTION = "add_action";
	public static final String ACTION_ANALYZE = "analyze";
	public static final String ACTION_AUTO = "auto";
	public static final String ACTION_CLICK = "click";
	public static final String ACTION_CONNECT = "connect";
	public static final String ACTION_DECOUPLE = "decouple";	
	public static final String ACTION_DROP = "drop";
	public static final String ACTION_EMERGENCY = "emergency";
	public static final String ACTION_FASTER10 = "faster10";
	public static final String ACTION_FREE = "free";
	public static final String ACTION_MOVE = "move";
	public static final String ACTION_OPEN = "open";
	public static final String ACTION_POWER = "power";
	public static final String ACTION_PROPS = "props";
	public static final String ACTION_QUIT = "quit";
	public static final String ACTION_SAVE = "save";
	public static final String ACTION_SLOWER10 = "slower10";
	public static final String ACTION_START = "start";
	public static final String ACTION_STOP = "stop";
	public static final String ACTION_TIMES = "update_times";
	public static final String ACTION_TOGGLE_F1 = "f1";
	public static final String ACTION_TOGGLE_F2 = "f2";
	public static final String ACTION_TOGGLE_F3 = "f3";
	public static final String ACTION_TOGGLE_F4 = "f4";
	public static final String ACTION_TURN = "turn";
	public static final String ACTION_UPDATE = "update";

	public static final String REALM           = "realm";
	public static final String REALM_ACTIONS   = "actions";
	public static final String REALM_APP       = "application";
	public static final String REALM_CAR       = "car";
	public static final String REALM_CONDITION = "condition";
	public static final String REALM_CONTACT   = "contact";
	public static final String REALM_CU        = "cu";
	public static final String REALM_LOCO      = "loco";
	public static final String REALM_ROUTE     = "route";
	public static final String REALM_PLAN      = "plan";
	public static final String REALM_TRAIN     = "train";
	
	public static final String  ASSIGN = "assign";
	public static final String  BLOCK               = "block";
	public static final String  COL                 = ": ";
	public static final String  CONTACT             = "contact";
	public static final String  CONTEXT             = "context";
	public static final String  DEFAULT_SPEED_UNIT  = "km/h";
	public static final String  DEFAULT_LENGTH_UNIT = "mm";
	public static final String  DISABLED            = "disabled";
	public static final String  DIRECTION            = "direction";
	public static final String  GITHUB_URL          = "https://github.com/srsoftware-de/Web4Rail";
	public static final String  ID                  = "id";
	public static final String  LOCKED              = "locked";
	public static final String  NAME			    = "name";
	public static final String  NBSP                = "&nbsp;";
	public static final String  NOTES               = "notes";
	public static final String  OCCUPIED            = "occupied";
	public static final String  PARENT              = "parent";
	public static final String  PORT                = "port";
	public static final String  RELAY               = "relay";
	public static final String  RESERVED            = "reserved";
	public static final String  ROUTE               = "route";
	public static final String  STATE               = "state";
	public static final String  TURNOUT             = "turnout";
	public static final String  TYPE                = "type";
	public static final Charset UTF8                = StandardCharsets.UTF_8;
}
