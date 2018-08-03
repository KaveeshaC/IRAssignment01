package cs276.assignments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class PostingList {

	private int termId;
	/* A list of docIDs (i.e. postings) */
	private List<Integer> postings;

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

	public PostingList(int termId) {
		this.termId = termId;
		this.postings = new ArrayList<Integer>();
	}

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}
	
	private static <X> X popNextOrNull(Iterator<X> p) {
        return p.hasNext() ? p.next() : null;
    }
	
	
	public static List<Integer> intersect(List<Integer> list, PostingList p) {
        List<Integer> res = new ArrayList<Integer>();
        if (p.getList().isEmpty()) return res;
        Iterator<Integer> iter1 = list.iterator();
        Iterator<Integer> iter2 = p.getList().iterator();
        Integer docId1 = popNextOrNull(iter1);
        Integer docId2 = popNextOrNull(iter2);
        while (docId1 != null && docId2 != null) {
            if (docId1 == docId2) {
                res.add(docId1);
                docId1 = popNextOrNull(iter1);
                docId2 = popNextOrNull(iter2);
            } else if (docId1 < docId2) {
                docId1 = popNextOrNull(iter1);
            } else {
                docId2 = popNextOrNull(iter2);
            }
        }
        return res;
    }
}
