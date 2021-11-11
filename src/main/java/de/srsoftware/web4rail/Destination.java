package de.srsoftware.web4rail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.BaseClass.Id;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;

public class Destination {
	private static final Logger LOG = LoggerFactory.getLogger(Destination.class);

	private Direction enterDirection;
	public Block block;

	private boolean shunting = false;

	private boolean turn = false;

	public Destination(Block block, Direction enterFrom) {
		if (block == null) throw new NullPointerException();
		this.block = block;
		this.enterDirection = enterFrom;
	}
	
	public Destination(Block block) {
		this(block,null);
	}
	
	@SuppressWarnings("unused")
	private Destination() {}

	boolean accepts(Direction enterDirection) {
		boolean result = this.enterDirection == null || enterDirection == null || this.enterDirection == enterDirection;
		LOG.debug(BaseClass.t(result ? "{} accepts train from {}" : "{} does not accept train from {}",this,enterDirection.inverse()));
		return result;
	}
	
	public String block() {
		return block.id().toString();
	};
	
	public static String dropFirstFrom(String tag) {
		if (BaseClass.isNull(tag)) return null;
		
		String[] parts = tag.split(Train.DESTINATION_PREFIX,3);
		if (parts.length<3) return null;

		return Train.DESTINATION_PREFIX+parts[2];
	}
	
	public Destination enterFrom(Direction enterFrom) {
		this.enterDirection = enterFrom;
		return this;
	}

	public Direction enterFrom() {
		return enterDirection;
	}
	
	public static Destination from(String tag) {
		if (BaseClass.isNull(tag)) return null;

		LOG.debug("→ processing \"{}\"...",tag);		
		String[] parts = tag.split(Train.DESTINATION_PREFIX,3);
		if (parts.length<2) return null;
		
		String firstTag = parts[1];
		LOG.debug("processing first tag: {}",firstTag);
		if (firstTag.length()<1) return null;
		int pos = firstTag.indexOf(Train.FLAG_SEPARATOR);
		String blockId = pos<0 ? firstTag : firstTag.substring(0,pos);
		
		Block block = Block.get(new Id(blockId));
		if (block == null) return null;
		
		Destination destination = new Destination(block);
		if (pos>0) {
			String modifiers = firstTag.substring(pos+1);
			for (int i=0; i<modifiers.length(); i++) {
				switch (modifiers.charAt(i)) {
					case Train.SHUNTING_FLAG: destination.shunting(true); break;
					case Train.TURN_FLAG: destination.turn(true); break;
					case '→': destination.enterFrom(Direction.WEST); break;
					case '←': destination.enterFrom(Direction.EAST); break;
					case '↓': destination.enterFrom(Direction.NORTH); break;
					case '↑': destination.enterFrom(Direction.SOUTH); break;
				}				
			}			
		}
		
		return destination;
	}

	
	public Destination shunting(boolean enable) {
		this.shunting = enable;
		return this;
	}
	
	public boolean shunting() {
		return shunting;
	}

	public String tag() {
		StringBuilder flags = new StringBuilder();
		if (turn) flags.append(Train.TURN_FLAG);
		if (shunting) flags.append(Train.SHUNTING_FLAG);
		if (enterDirection == Direction.EAST) flags.append('←');
		if (enterDirection == Direction.WEST) flags.append('→');
		if (enterDirection == Direction.NORTH)flags.append('↓');
		if (enterDirection == Direction.SOUTH) flags.append('↑');
		StringBuilder sb = new StringBuilder();
		sb.append('@').append(block());
		if (flags.length()>0) sb.append('+').append(flags);
		return sb.toString(); 
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (enterDirection == null) {
			sb.append(block);
		} else {
			sb.append(BaseClass.t("{} from {}",block,enterDirection));
		}
		return sb.toString();
	}
	
	public void turn(boolean enable) {
		this.turn  = enable;
	}
	
	public boolean turn() {
		return turn;
	}
}
