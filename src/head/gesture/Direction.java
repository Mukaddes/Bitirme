package head.gesture;

public class Direction implements Comparable<Direction>{
	String name;
	int value;
	
	public Direction(String n, int v){
		this.name = n;
		this.value = v;
	}
	
	@Override
	public int compareTo(Direction another) {
		if(this.value < another.value)
			return -1;
		if(this.value > another.value)
			return 1;
	
		return 0;
	}
}