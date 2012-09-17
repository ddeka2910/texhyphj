package net.davidashen.text;

import java.util.Hashtable;
import java.util.Map;

import net.davidashen.util.List;

/**
 * Tree structure for representing hyphenation rules in a type safe manner.
 */
public class TreeNode {
	final private String segment;
	final private int[] hyphenation;
	final private boolean blank;
	final private Map<Character, TreeNode> children = new Hashtable<Character, TreeNode>();

	/**
	 * Create a root node to create all other nodes inside.
	 */
	public static TreeNode createRoot() {
		return new TreeNode("");
	}
	
	/**
	 * Create a new node from a string pattern
	 */
	public static TreeNode createFromPattern(String pattern) {
		char[] patternChars = pattern.toCharArray();

		int characterCount = 0;
		char[] segmentChars = new char[patternChars.length]; 
		int[] hyphenations = new int[patternChars.length+1];
		
		
		for (char c : patternChars) {
			if(Character.isDigit(c)) {
				hyphenations[characterCount] = Integer.parseInt("" +  c);
			} else {
				segmentChars[characterCount++] = c;
			}
		}
		
		return new TreeNode(
				String.copyValueOf(segmentChars, 0, characterCount), 
				copyOfRange(hyphenations, 0, characterCount+1)
				);
	}
	
	/**
	 * Arrays.copyOfRange() was implemented in Java 6, and we still need Java 5 as the minimum requirement. 
	 */
	private static int[] copyOfRange(int[] srcArray, int offset, int length) {
		int[] destArray = new int[length];
		for(int i = offset; i < length; i++) {
			destArray[i] = srcArray[offset + i];
		}
		return destArray;
	}

	/**
	 * Create a node with no hyphenation information (a blank node).
	 */
	public TreeNode(String segment) {
		this.segment = segment;
		this.hyphenation = new int[segment.length() + 1];
		this.blank = true;
	}

	/**
	 * Create a node with hyphenation information.
	 */
	public TreeNode(String segment, int[] hyphenationData) {
		this.segment = segment;
		this.hyphenation = hyphenationData;
		this.blank = false;
	}

	/**
	 * Add a child rule to this node. The child node must match a longer, more
	 * specialized, segment than the segment of the node it is added to.
	 * Grandchildren will recursively added to children of this node.
	 * 
	 * @param segment
	 *            The string of text that this rule matches against
	 * @param hyphenation
	 *            They hypenation information for this match
	 */
	public void createChild(String segment, int[] hyphenation) {
		if (!segment.startsWith(this.segment)) {
			throw new IllegalArgumentException("Can not add child \'" + segment
					+ "\' to parent \'" + this.segment + "\'");
		}

		TreeNode node = new TreeNode(segment, hyphenation);

		if (segment.length() == this.segment.length() + 1) {
			children.put(segment.charAt(segment.length() - 1), node);
		} else {
			final char keyCharacter = segment.charAt(this.segment.length());
			if(!hasChild(keyCharacter)){
				addBlankChild(keyCharacter);
			}
			getChild(keyCharacter).createChild(segment, hyphenation);
		}
	}
	
	/**
	 * Add place holder node required by tree structure. 
	 */
	private void addBlankChild(char nodeCharacter) {
		children.put(nodeCharacter, new TreeNode(this.segment + nodeCharacter));
	}

	/**
	 * Parse pattern and add the resulting segment and hyphernation to the tree.
	 */
	public void createChildFromPattern(String pattern) {
		TreeNode tmpNode = TreeNode.createFromPattern(pattern); 
		this.createChild( tmpNode.getSegment(), tmpNode.getHyphenation());
	}

	
	public String getSegment() {
		return segment;
	}

	public char getLastCharacter() {
		return segment.charAt(segment.length() - 1);
	}

	public int[] getHyphenation() {
		return hyphenation;
	}

	public boolean hasChild(char c) {
		return children.containsKey(c);
	}
	
	public TreeNode getChild(char c) {
		return children.get(c);
	}

	/**
	 * Is this the root node onto which all other nodes should be added?
	 */
	public boolean isRoot() {
		return this.segment == "";
	}

	/**
	 * Node is only a place holder required by the tree structure.
	 */
	public boolean isBlank() {
		return this.blank;
	}

	/**
	 * Create texhyphj original (lisp like) List structure from node tree for consumption by Hypernator  
	 */
	public List toList() {
		List list = new List();
		list.snoc(new Character(getLastCharacter()));
		list.snoc(getHyphenation());
		
		for(Character c : children.keySet()) {
			list.snoc(children.get(c).toList());
		}
		return list;
	}
}
