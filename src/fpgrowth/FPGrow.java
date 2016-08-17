package fpgrowth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class FPGrow {
	FPNode root;
	int min_sup = 3;
	HashMap<String, Header> headerTable;
	Map<List<String>, Integer> frequentMap = new HashMap<List<String>, Integer>();
//	Map<Set<String>, Integer> frequentMap = new HashMap<Set<String>, Integer>();
	
	public void FPGrowthAlgorithm(List<List<String>> transactions){
		HashMap<String, Integer> itemCount = getFreqCount(transactions);
		
		//Sort items according to itemCount
		for(List<String> transaction: transactions){
			Collections.sort(transaction, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					// TODO Auto-generated method stub
					if(itemCount.get(o1)>itemCount.get(o2))return -1;
					else if(itemCount.get(o1)<itemCount.get(o2)) return 1;
					return 0;
				}
			});
		}
		
		//build tree
		FPGrowth(transactions, null);
	}
	
	/**
	 * For test, print headers
	 * @param headers
	 */
	private void testHeadTable(HashMap<String, FPNode> headers){
		if(headers==null) return;
		for(Entry<String, FPNode> entry : headers.entrySet()){
			String headerName = entry.getKey();
			int supp = headers.get(headerName).support;
			StringBuffer buff = new StringBuffer();
			FPNode currPointer = entry.getValue().next;
			while(currPointer!=null){
				buff.append(currPointer.itemName+"("+currPointer.support+")---->");
				currPointer = currPointer.next;
			}
			
			System.out.println(headerName+"("+supp+") : "+buff.toString());
		}
	}
	
	public void getCombinations(ArrayList<FPNode> path, List<List<FPNode>> combinations){
		if(path==null || path.size()==0)return;
		int length = path.size();
		for(int i = 1;i<Math.pow(2, length);i++){
			String bitmap = Integer.toBinaryString(i);
//			System.out.println(i+"\t"+bitmap);
			List<FPNode> combine = new ArrayList<>();
			for(int j = 0;j<bitmap.length();j++){
				if(bitmap.charAt(j)=='1'){
					combine.add(path.get(length-bitmap.length()+j));
				}
			}
			combinations.add(combine);
		}
	}
	
	
	
	public void FPGrowth(List<List<String>> transactions, List<String> postModel){
		Map<String, Integer> itemCount = getFreqCount(transactions);
		Map<String, FPNode> headerTable = new HashMap<>();
		
		// set header table
		for(Entry<String, Integer> entry:itemCount.entrySet()){
			String itemName = entry.getKey();
			Integer count = entry.getValue();
			
			//check the min_support
			if(count>=this.min_sup){
				FPNode node = new FPNode(itemName);
				node.support = count;
				headerTable.put(itemName, node);
			}
		}
		
		FPNode root = buildTree(transactions, itemCount, headerTable);
		
		if(root==null) return;

		if(root.children==null || root.children.size()==0) return;
		
		//optimization for single path
		if(isSingleBranch(root)){
			ArrayList<FPNode> path = new ArrayList<>();
			FPNode curr = root;
			while(curr.children!=null && curr.children.size()>0){
				String childName = curr.children.keySet().iterator().next();
				curr = curr.children.get(childName);
				path.add(curr);
			}
			
			List<List<FPNode>> combinations = new ArrayList<>();
			getCombinations(path, combinations);
			
			for(List<FPNode> combine : combinations){
				int supp = 0;
				List<String> rule = new ArrayList<>();
				for(FPNode node : combine){
					rule.add(node.itemName);
					supp = node.support;
				}
				if(postModel!=null){
					rule.addAll(postModel);
				}
				
				frequentMap.put(rule, supp);
			}
			
			return;
		}
		
		for(FPNode header : headerTable.values()){
			
			List<String> rule = new ArrayList<>();
			rule.add(header.itemName);// header is item >= min_support
			
			if (postModel != null) {
                rule.addAll(postModel);
            }
			
			frequentMap.put(rule, header.support);
			
			List<String> newPostPattern = new ArrayList<>();
			newPostPattern.add(header.itemName);
            if (postModel != null) {
                newPostPattern.addAll(postModel);
            }
            
            //new conditional pattern base
            List<List<String>> newCPB = new LinkedList<List<String>>();
            FPNode nextNode = header.next;
			while((nextNode = nextNode.next)!=null){
				int leaf_supp = nextNode.support;
				
				//get the path from root to this node
				List<String> path = new ArrayList<>();
				FPNode parent = nextNode;
				while(!(parent = parent.parent).itemName.equals("ROOT")){
					path.add(parent.itemName);
				}
				if(path.size()==0)continue;
				while(leaf_supp-- >0){
					newCPB.add(path);
				}
			}
			FPGrowth(newCPB, newPostPattern);
		}
	}
	
	public FPNode buildTree(List<List<String>> transactions, final Map<String, Integer> itemCount, final Map<String, FPNode> headerTable){
		
		FPNode root = new FPNode("ROOT");
		root.parent = null;
		
		for(List<String> transaction : transactions){
			FPNode prev = root;
			HashMap<String, FPNode> children = prev.children;
			
			for(String itemName:transaction){
				//not in headerTable, then not qualify the min support.
				if(!headerTable.containsKey(itemName))continue;
				
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
					FPNode header = headerTable.get(itemName);
					if(header!=null){
						header.attach(t);
					}
					
				}
				prev = t;
				children = t.children;
			}
		}
		
		return root;
		
	}
	
	 private boolean isSingleBranch(FPNode root) {
	        boolean rect = true;
	        while (root.children != null && root.children.size()>0) {
	            if (root.children.size() > 1) {
	                rect = false;
	                break;
	            }
	            
	            
	            String childName = root.children.keySet().iterator().next();
	            root = root.children.get(childName);
	        }
	        return rect;
	    }
	

	
	private HashMap<String, Integer> getFreqCount(List<List<String>> transactions){
		HashMap<String, Integer> itemCount = new HashMap<String, Integer>();
		for(List<String> transac: transactions){
			for(String item: transac){
				if(itemCount.containsKey(item)){
					int count = itemCount.get(item);
					itemCount.put(item, ++count);
				}
				else{
					itemCount.put(item, 1);
				}
			}
		}
		
//		for(Entry<String, Integer> entry:itemCount.entrySet())System.out.println(entry.getKey()+"\t"+entry.getValue());
		return itemCount;
	}
	
	public List<List<String>> loadTransactions(String filename) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		List<List<String>> transactions = new ArrayList<>();
		
		//remove gain= loss=
		Pattern pattern = Pattern.compile("gain=\\w*|loss=\\w*");
		
		
		String newline;
		while((newline = br.readLine())!=null){
			Matcher matcher = pattern.matcher(newline);
			newline = matcher.replaceAll("");
			newline = newline.replaceAll("( )+", " "); //remove multiple spaces
			String[] items = newline.split(" ");
			transactions.add(new ArrayList<String>(Arrays.asList(items)));
		}
		br.close();
		
		return transactions;
	}
	
	private void sortItemset(String filename) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		HashMap<String, Integer> itemCount = new HashMap<String, Integer>();
		
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
	
	
	
	public void printResult(){
		for(Entry<List<String>, Integer> entry : this.frequentMap.entrySet()){
			List<String> rule = entry.getKey();
			Integer support = entry.getValue();
			System.out.println(Arrays.toString(rule.toArray())+"\t\t"+support);
		}
	}
	
	

	
	public static void main(String[] args) throws IOException {
		String infile = "./data/census-sample20.dat";
		FPGrow model = new FPGrow();
//		List<List<String>> transactions = model.loadTransactions(infile);
//		model.FPGrowthAlgorithm(transactions);
//		
//		model.printResult();
		
		ArrayList<FPNode> nodes = new ArrayList<>();
		nodes.add(new FPNode("a"));
		nodes.add(new FPNode("b"));
		nodes.add(new FPNode("c"));
		
		List<List<FPNode>> combinations = new ArrayList<>();
		model.getCombinations(nodes, combinations);
		
		for(List<FPNode> comb:combinations){
			System.out.println(Arrays.toString(comb.toArray()));
		}
		
		
		
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
