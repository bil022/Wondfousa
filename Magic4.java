import java.io.*;
import java.util.*;

public class Magic4 {
	private final static boolean OVER_WRITE = false;
	private final static int TOO_BIG = 100000;
	
	// new Finder().scan(new File("."));
	class Finder {
		LinkedList<File> files = new LinkedList<File>();

		public void scan(File dir) {
			if (!dir.isDirectory()) {
				String path=dir.getParent();
				if (dir.getName().endsWith(".txt")) {
					if (!new File(path+"/processed.xls").exists() || OVER_WRITE)
					{
						//System.err.println(path+"/processed.xls not found");
						System.err.println("Scanning: "+dir);
						process(""+dir);
						files.add(dir);
					}
				}
			} else {
				for (File file : dir.listFiles()) {
					scan(file);
				}
			}
		}

		HashMap<String, LinkedList<Float> > lefts, mids, rights;
		public void process(String txt) {
			String name=txt;
			if (lefts==null) {
				lefts= new HashMap<String, LinkedList<Float> >();
				mids= new HashMap<String, LinkedList<Float> >();
				rights= new HashMap<String, LinkedList<Float> >();
			}
			String line="";
			try {
				Scanner s = new Scanner(new BufferedReader(new FileReader(txt)));
				float prev_L=0, prev_M=0, prev_R=0;
				float sum_L=0, sum_M=0, sum_R=0;
				while (s.hasNext()) {
					line=s.nextLine();
					if (line.startsWith("Date"))
						continue;
					String[] data=line.split("\t");
					if (data.length!=6)
						continue;
					float left=Float.parseFloat(data[3]);
					float mid=Float.parseFloat(data[4]);
					float right=Float.parseFloat(data[5]);
					if (left==0) left=prev_L;
					if (mid==0) mid=prev_M;
					if (right==0) right=prev_R;
					sum_L+=left; sum_M+=mid; sum_R+=right;

					if (sum_L!=0) {
						if (!lefts.containsKey(name))
							lefts.put(name, new LinkedList<Float>());
                        //System.err.println("add:\t"+left);
						lefts.get(name).add(left);
					}
					if (sum_M!=0) {
						if (!mids.containsKey(name))
							mids.put(name, new LinkedList<Float>());
						mids.get(name).add(mid);
					}
					if (sum_R!=0) {
						if (!rights.containsKey(name))
							rights.put(name, new LinkedList<Float>());
						rights.get(name).add(right);
					}

					prev_L=left; prev_M=mid; prev_R=right;	
				}
                // System.err.println("prev_L: "+prev_L);
			} catch (Exception exc) {
				System.err.println(txt+":"+line);
				System.err.println(exc);
				System.err.println();
			}
		}
	}

	class Record implements Comparable<Record> {
		String file;
		String pos;
		ArrayList<Float> data;
		ArrayList<Float> delta;
		ArrayList<Float> aligned;

		@Override
			public int compareTo(Record rec) {
				int diff=file.compareTo(rec.file);
				if (diff!=0)
					return diff;
				return pos.compareTo(rec.pos);
			}

		public String id() {
			return file+":"+pos;
		}
		public float getAt(int index, float na) {
			if (index>=aligned.size())
				return na;
			return aligned.get(index);
		}
		public Record(String txt, String tag) {
			file=txt; pos=tag;
			data=new ArrayList<Float>();
			delta=new ArrayList<Float>();
			aligned=new ArrayList<Float>();
		}
		void add(float val) {
			data.add(val);
		}
		float sum() {
			Float s=0f;
			for (Float f:data) {
				s+=f;
			}
			return s;
		}
		float sum25_75pctAligned() {
			Float s=0f; int i=0, n=asize(), low=n/4, high=n*3/4;
			for (Float f:aligned) {
				if (i>=low && i<high)
					s+=f;
				i++;
			}
			return s;
		}
		int posOfMinDelta() {
			int i, n=delta.size(), ret=0;
			float val=0;
			for (i=0; i<n; i++) {
				if (delta.get(i)<val) {
					ret=i; val=delta.get(i);
				}
			}
			return ret;
		}
		void process() {
			int n=data.size();
			if (n==0) return;
			float p0=data.get(0), prev=p0;
            //System.err.println("p0: "+p0);
			for (Float f:data) {
                // System.err.println("Raw:\t"+f);
				delta.add(f-prev);
				prev=f;
			}
			int anchor=posOfMinDelta();
			// System.err.println(file+" "+pos+" -> "+data.size()+" @ " + anchor+"/"+data.size());
			int SHIFT=2;
			if (anchor<SHIFT) {
				for (int i=0; i<SHIFT-anchor; i++)
					aligned.add(p0);
				for (Float f:data)
					aligned.add(f);
			} else {
				// System.out.println("Adding @ "+(anchor-10)+" for "+file);
				for (int i=anchor-SHIFT; i<n; i++)
					aligned.add(data.get(i));
				// aligned.addAll(anchor-10, data);
			}
			float avg=sum25_75pctAligned()*2/aligned.size();
			//System.err.println(file+" avg "+avg+"\n");
			int nLarge=0;
			for (int i=0; i<aligned.size(); i++) {
				//aligned.set(i, aligned.get(i)/avg);
				aligned.set(i, aligned.get(i)-avg);
				if (aligned.get(i)>TOO_BIG)
					nLarge++;
			}
			if (nLarge>0)
				System.err.println(nLarge+" records >= "+TOO_BIG+ " in "+file); // check aligned here
		}
		int size() { return data.size(); }
		int asize() { return aligned.size(); }
	}
	class Records extends Hashtable<String, ArrayList<Record> > {
		int max_row=0; float NA=-9999999999f;
		public String getOutputFile(String file) {
			//System.err.println("?"+file);

			String output=new File(file).getParent();
			//System.err.print(output+"->");
			//output=output.replaceAll("[^A-Za-z0-9]+", "_");
			//output=output.substring(1)+".xls";
			output+="/processed.xls";
			//System.err.println(file+"->"+output);
			return output;
		}
		public void add(Record e) {
			if (e.size()>5000) {
				System.err.println("File too large, skipped "+e.file);
				return;
			}
			e.process();
			if (max_row<e.asize())
				max_row=e.asize();
			String output=getOutputFile(e.file);
			if (!containsKey(output))
				put(output, new ArrayList<Record>());
			get(output).add(e);
		}

		public void excel() {
			// System.err.println("Max row: "+max_row);
			// Hashtable<String, ArrayList<Record>

			//String[] keys = keySet().toArray(new String[0]);

			for (String k:keySet()) {
				if (file_exists(k) && !OVER_WRITE)
					continue;
				System.err.println(k);
				Collections.sort(get(k));

				try {
					FileWriter fw = new FileWriter(k);
					BufferedWriter bw = new BufferedWriter(fw);
					//int idx=0;
					String copyTo=null;
					for (Record r:get(k)) {
						if (copyTo==null) {
							copyTo=getOutputFile(r.file);
						}
						//if (idx++>0)
                        bw.append("\t");
						bw.append(r.id());
					}
					bw.append("\n");

					for (int i=0; i<max_row; i++) {
						//idx=0;
                        bw.append(String.valueOf(i*0.35));
						for (Record r:get(k)) {
							//if (idx++>0)
                            bw.append("\t");
							float v=r.getAt(i, NA);
							if (v!=NA)
								bw.append(String.valueOf(v));
						}
						bw.append("\n");
					}
					bw.close();
					fw.close();

					//System.out.println("cp "+k+" "+raw_path+"/"+copyTo);
				}	catch (Exception exc) {
					System.err.println(exc);
				}
			}
		}
	}
	static String getString(Scanner s) throws IOException {
		if (!s.hasNext()) throw new IOException("EOF");
		return s.next();
	}
	static float getFloat(Scanner s) throws IOException {
		if (!s.hasNextFloat()) throw new IOException("nextFloat?");
		return s.nextFloat();
	}
	static int getInt(Scanner s) throws IOException {
		if (!s.hasNextInt()) throw new IOException("nextInt?");
		return s.nextInt();
	}
	public static void main(String[] args) throws IOException {
		new Magic4();
	}
	public static boolean file_exists(String path) {
		File f = new File(path);
		return f.exists();
	}

	String raw_path;

	public Magic4() throws IOException {
		Finder finder=new Finder();
		finder.scan(new File("."));
		Records records=new Records();
		if (finder.lefts!=null)
			process(finder.lefts, records, "L");
		if (finder.mids!=null)
			process(finder.mids, records, "M");
		if (finder.rights!=null)
			process(finder.rights, records, "R");
		records.excel();
	}

	public void process(HashMap<String, LinkedList<Float> > data, Records records, String tag) {
		Set set=data.entrySet();
		Iterator itr=set.iterator();

		while (itr.hasNext()) {
			Map.Entry entry=(Map.Entry)itr.next();
			String txt=(String)entry.getKey();
			LinkedList<Float> lst=data.get(txt);
			Record rec=new Record(txt, tag);
			//System.err.println("Got: "+txt);
			for (int i=0; i<lst.size(); i++) {
				rec.add(lst.get(i));
			}
			records.add(rec);
		}
	}
}
