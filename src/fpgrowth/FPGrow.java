package fpgrowth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FPGrow {
	FPNode root;
	int min_sup = 3;
	HashMap<String, Integer> itemCount;
	HashMap<String, Header> headerTable;
	
	private void sortItemset(String filename) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		itemCount = new HashMap<String, Integer>();
		
		String newline;
		while((newline = br.readLine())!=null){
			String[] items = newline.split(" ");
			for(String item:items){
				if(itemCount.containsKey(item)){
					int count = itemCount.get(item);
					itemCount.put(item, ++count);
				}
				else{
					itemCount.put(item, 1);
				}
			}
		}
		br.close();

	}
	
	public void getPath(String item){
		FPNode firstNode = this.headerTable.get(item).next;
		while(firstNode!=null){
			Stack<FPNode> stack = new Stack<FPNode>();
			FPNode tail = firstNode;
			while(!tail.itemName.equals("$")){
				stack.push(tail);
				tail = tail.parent;
			}
			while(!stack.isEmpty())System.out.print(stack.pop().itemName+"\t");
			System.out.println();
			firstNode = firstNode.next;
		}
	}
	
	public void createTree(String filename) throws IOException{
		sortItemset(filename);
		// insert tree
		this.headerTable = new HashMap<String, Header>();
		this.root = new FPNode("ROOT");
		this.root.parent = new FPNode("$");
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		String newline;
		while((newline = br.readLine())!=null){
			insert(newline);
		}
		
		Iterator<String> it = this.headerTable.keySet().iterator();
		while(it.hasNext()){
			String itemName = it.next();
			Header header = this.headerTable.get(itemName);
			header.sumSup();
			this.headerTable.put(itemName, header);
		}
		
		br.close();
//		printFPTree(root,0);
//		printHeadTable();
	}
	
	public void printFPTree(FPNode node, int level){
		if(node!=null){
			System.out.print(node.itemName+"\tlevel="+level+"  parent=");
		}
		if(node.parent!=null)System.out.println(node.parent.itemName);
		else System.out.println("NULL");
		if(node.children==null)System.out.println(" --LEAF");
		Iterator<String> it = node.children.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			FPNode nextNode= node.children.get(name);
			
			printFPTree(nextNode,++level);
		}
	}
	
	public void printHeadTable(){
		if(this.headerTable==null)return;
		Iterator<String> it = this.headerTable.keySet().iterator();
		while(it.hasNext()){
			String itemName = it.next();
			int sup = this.headerTable.get(itemName).support;
//			System.out.println(itemName+"\t"+sup);
			FPNode node = headerTable.get(itemName).next;
			System.out.print(sup+" : "+itemName+" : ");
			while(node!=null){
				System.out.print(node.toString()+"\t");
				node = node.next;
			}
			System.out.println();
		}
	}
	
	/**
	 * Add items from one transection into the FP tree
	 * @param transLine
	 */
	private void insert(String transLine){
		if(this.root==null)return;
//		if(this.itemRank==null)return;
		if(transLine==null || transLine.trim().equals(""))return;
		
		//remove gain= loss=
		Pattern pattern = Pattern.compile("gain=\\w*|loss=\\w*");
		Matcher matcher = pattern.matcher(transLine);
		String newLine = matcher.replaceAll("");
		
		// order by support
		ArrayList<Pair> itemset = new ArrayList<Pair>();
		String[] items = newLine.split(" ");
		for (String item: items){
			if( item.trim().equals("")) continue;
			if( this.itemCount.get(item) > this.min_sup){
				itemset.add(new Pair(item, this.itemCount.get(item)));
			}
		}
		Collections.sort(itemset, new PairComparator());
		
		//check correct
//		System.out.println(transLine);
//		for(Pair pair:itemset){
//			System.out.print(pair.name+"\t");
//		}
//		System.out.println();
		
		FPNode prev = this.root;
		HashMap<String, FPNode> children = prev.children;
		for(Pair item:itemset){
			String itemName = item.name;
			FPNode t;
			if(children.containsKey(itemName)){
				children.get(itemName).support++;
				t = children.get(itemName);
			}
			else{
				t = new FPNode(itemName);
				t.parent = prev;
				children.put(itemName, t);
				
				//add to header
				if(this.headerTable.containsKey(itemName)){
					this.headerTable.get(itemName).attach(t);
				}
				else{
					Header header = new Header(t);
					this.headerTable.put(itemName, header);
				}
			}
			prev = t;
			children = t.children;
		}
	}
	
	

	
	public static void main(String[] args) throws IOException {
		String infile = "./data/census-sample20.dat";
		FPGrow model = new FPGrow();
		model.createTree(infile);
		model.getPath("occupation=Exec-managerial");

	}
}

class Pair{
	String name;
	int count;
	public Pair(String name, int count) {
		super();
		this.name = name;
		this.count = count;
	}
	@Override
	public String toString() {
		return "Pair [name=" + name + ", count=" + count + "]";
	}
	
	
	
}

class PairComparator implements Comparator<Pair>{

	public int compare(Pair o1, Pair o2) {
		// TODO Auto-generated method stub
		if(o1.count < o2.count) return 1;
		else if(o1.count > o2.count)return -1;
		return 0;
	}
	
}

class Header{
	int support;
	FPNode next;
	
	public Header(FPNode firstNode) {
		// TODO Auto-generated constructor stub
		this.support=firstNode.support;
		this.next = firstNode;
	}
	
	
	/**
	 * Attach new FPNode to the tail of header table
	 * @param newNode
	 * @param support
	 */
	public void attach(FPNode newNode){
		
//		this.support+=newNode.support;
		FPNode node = this.next;
		while(node.next!=null){
			node = node.next;
		}
		node.next = newNode;
	}
	
	public void sumSup(){
		int sup = 0;
		FPNode node = this.next;
		while(node!=null){
			sup+=node.support;
			node = node.next;
		}
		this.support = sup;
	}


	@Override
	public String toString() {
		return "Header [support=" + support + ", next=" + next + "]";
	}
	
	
	
}
