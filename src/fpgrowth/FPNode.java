package fpgrowth;

import java.util.HashMap;

public class FPNode {
	
	int support;
	String itemName;
	HashMap<String, FPNode> children;
	boolean isLeaf;
	FPNode next; //use for header table
	FPNode parent;
	
//	public FPNode(){
//		this.children =  new HashMap<String, FPNode>();
//		this.next = null;
//	}
	public FPNode(String name) {
		this.itemName = name;
		this.support = 1;
		this.children =  new HashMap<String, FPNode>();
		this.next =null;
		this.parent = null;
	}

	@Override
	public String toString() {
		return "FPNode [support=" + support + ", itemName=" + itemName + "]";
	}
	
	
}


