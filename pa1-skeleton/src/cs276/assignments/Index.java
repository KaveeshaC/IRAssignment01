package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import java.util.LinkedList;
import java.util.List;
import cs276.util.TermDocComparator;


import java.util.*;



public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the given file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws IOException {
		/*
		 * TODO: Your code here
		 *	 
		 */
	
		//updating the postingDict
		postingDict.put(posting.getTermId(), new Pair<Long, Integer>(fc.position(), posting.getList().size()));
		try {
			index.writePosting(fc, posting); //writing posting to file channel
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	
	

    private static Integer popNextOrNull(Iterator<Integer> iter) {
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }

    /**
     * Merge two posting lists

     */
    
    //Merge posting function. merging Two posting lists p1,p2
    private static PostingList mergePostings(PostingList p1, PostingList p2) {
        Iterator<Integer> iter1 = p1.getList().iterator();
        Iterator<Integer> iter2 = p2.getList().iterator();
        List<Integer> postings = new ArrayList<Integer>();
        Integer docId1 = popNextOrNull(iter1);
        Integer docId2 = popNextOrNull(iter2);
        Integer prevDocId = 0;
        while (docId1 != null && docId2 != null) {
            if (docId1.compareTo(docId2) < 0) {
                if (prevDocId.compareTo(docId1) < 0) {
                    postings.add(docId1);
                    prevDocId = docId1;
                }
                docId1 = popNextOrNull(iter1);
            } else {
                if (prevDocId.compareTo(docId2) < 0) {
                    postings.add(docId2);
                    prevDocId = docId2;
                }
                docId2 = popNextOrNull(iter2);
            }
        }

        while (docId1 != null) {
            if (prevDocId.compareTo(docId1) < 0) {
                postings.add(docId1);
            }
            docId1 = popNextOrNull(iter1);
        }

        while (docId2 != null) {
            if (prevDocId.compareTo(docId2) < 0) {
                postings.add(docId2);
            }
            docId2 = popNextOrNull(iter2);
        }

        return new PostingList(p1.getTermId(), postings);
    }

	
	
	//Check whether there is 3 argument passed
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println(" Num of arguments");
			return;
		}
		

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index"; //Class path of the Basic Index
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();    //Creating a Instance of the Basic Index
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		
		//Getting the input directory path and save it to  String
		String root = args[1];
		File rootdir = new File(root); //Creating a File object
		
		//Check whether there is a existing input directory. if not return a Error message
		//IF there is a error program will stop
		
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		
		//Getting the path of Output Directory and save it to String
		String output = args[2];
		File outdir = new File(output); //Creating a Output File Object
		
		//Check whether there is existing output directory.
		//IF output folder exists and it is not a directory return a error message
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		//if there is no output Directory Create a New Output Directory
		//If cannot create return a Error Message
		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* A filter to get rid of all files starting with .*/
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) { //getting the paths of sub directory's in Input folder and save it to a String 
				String name = pathname.getName();
				return !name.startsWith(".");
			}
		};

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles(filter); //get the directory list from the input data folder(folder 0,1,2,..)
		
		/*new one*/
		// use ArrayList to collect all termID-docID pairs
        List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();

		/* For each block */
        
        //creating a Block from the Directory list
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile); //put that created block file into the block Queue

			/*new one here*/
			
			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles(filter); //creating a file list from the (Input folder -> 0 ->....) files that inside the "0" Directory
			
			
			
			
			/* For each file */
			for (File file : filelist) {  // loop to all the files inside "0" Folder
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();  //Block is 0 / name of the file, OUTPUT => 0/3dradiology.stanford.edu
				docDict.put(fileName, docIdCounter++); //creating a new Document ID and put that after the file name ( EX: 0/3dradiology.stanford.edu_	   2 ) 
													   
				
				
				/*new one*/
				int docId = ++docIdCounter;
                docDict.put(fileName, docId);  // Save it to a file
				
                
                
				
				BufferedReader reader = new BufferedReader(new FileReader(file)); //Reading the file Line By Line
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+"); 	// Split By Space and save it to a array
					for (String token : tokens) { 
						/*
						 * TODO: Your code here
						 *       For each term, build up a list of
						 *       documents in which the term occurs
						 */
						
						int termId;
						
						if (!termDict.containsKey(token)) { //In the Term Dictionary Words Cannot be Repeated Because of that  First We have to check
							
							termId = ++wordIdCounter;
                            termDict.put(token, termId); //Save the Term And Created ID in Term file (Ex: zumba	36484)
							
						}else {
                            termId = termDict.get(token);
                        }
						
						//Array list = pair
						pairs.add(new Pair(termId, docId));  //Save array list from termID and docID
						
						
					}
				}
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			
			/*
			 * TODO: Your code here
			 *       Write all posting lists for all terms to file (bfc) 
			 */

			//Collections.sort(pairs, new TermDocComparator());
            // sort pairs
            java.util.Collections.sort(pairs, new TermDocComparator());

            // write output
            int cnt = 0, prevTermId = -1, termId, prevDocId = -1, docId;
            if (pairs.size() > 0)
                // set valid prevTermID
                prevTermId = pairs.get(0).getFirst();

            List<Integer> postings = new ArrayList<Integer>();
            for (Pair<Integer, Integer> p : pairs) {
                termId = p.getFirst();
                docId = p.getSecond();

                if (termId == prevTermId) {
                    // duplicate docIDs only added once
                    if (prevDocId != docId) {
                        postings.add(docId);
                    }
                    prevDocId = docId;
                } else {
                    // a different term is encountered
                    // should write postings of previous term to disk
                    writePosting(bfc.getChannel(), new PostingList(prevTermId, postings));

                    // start new postings
                    postings.clear();
                    postings.add(docId);
                    prevTermId = termId;
                    prevDocId = docId;
                }
            }
            writePosting(bfc.getChannel(), new PostingList(prevTermId, postings));

            // clear the contents in pairs and close file channel
            pairs.clear();

			
			
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);
		System.out.println("Success");
		//Success
		
		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			
			File combfile = new File(output, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}

			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/*
			 * TODO: Your code here
			 *       Combine blocks bf1 and bf2 into our combined file, mf
			 *       You will want to consider in what order to merge
			 *       the two blocks (based on term ID, perhaps?).
			 *       
			 */
            FileChannel fc1 = bf1.getChannel();
            FileChannel fc2 = bf2.getChannel();
            FileChannel mfc = mf.getChannel();

            
    		try {
    			
    			
    			PostingList p1 = index.readPosting(fc1);
                PostingList p2 = index.readPosting(fc2);

                while (p1 != null && p2 != null) {
                    int t1 = p1.getTermId();
                    int t2 = p2.getTermId();

                    if (t1 == t2) {
                        // merge postings of the same term
                        PostingList p3 = mergePostings(p1, p2);

                        // write p3 to disk
                        writePosting(mfc, p3);
                        p1 = index.readPosting(fc1);
                        p2 = index.readPosting(fc2);
                    } else if (t1 < t2) {
                        // write p1
                        writePosting(mfc, p1);
                        p1 = index.readPosting(fc1);
                    } else {
                        // write p2
                        writePosting(mfc, p2);
                        p2 = index.readPosting(fc2);
                    }
                }

                while (p1 != null) {
                    writePosting(mfc, p1);
                    p1 = index.readPosting(fc1);
                }

                while (p2 != null) {
                    writePosting(mfc, p2);
                    p2 = index.readPosting(fc2);
                }
    			
    			
    			
    			bf1.close();
    			bf2.close();
    			mf.close();
    			b1.delete();
    			b2.delete();
    			blockQueue.add(combfile);
    			
    		} catch (Throwable e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} 
            
            
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
