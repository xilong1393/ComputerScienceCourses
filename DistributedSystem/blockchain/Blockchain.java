
/* the start up batch script is as follows:
 * javac -cp "gson-2.8.2.jar" Blockchain.java
 * start java -cp ".;gson-2.8.2.jar" Blockchain 0
 * start java -cp ".;gson-2.8.2.jar" Blockchain 1
 * start java -cp ".;gson-2.8.2.jar" Blockchain 2
*/
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;

public class Blockchain {
	static String defaultServer = "localhost";
	public static boolean start = false;
	static int ProcessID=0;
	//this is the canonical blockchain list, every new BlockRecord will be added to this list
	static List<BlockRecord> list=new ArrayList<>();
	static int numProcesses = 3;
	public static PublicKey PublicKey;
	public static PrivateKey PrivateKey;
	
	public static void main(String[] args){
		try 
		{
			ProcessID = (args.length < 1) ? 0 : Integer.parseInt(args[0]);
			System.out.println("Xilong Yu's Blockchain Application Started, ProcessID: "+ProcessID+"\n");
			PriorityBlockingQueue<BlockRecord> queue =new PriorityBlockingQueue<>();
			//set up the ports
			Ports ports=new Ports(ProcessID);
			KeyPair kv =BlockchainUtil.generateKeyPair();
			PublicKey =kv.getPublic();
			PrivateKey =kv.getPrivate();
			PublicKeyServer.PBlocks[ProcessID] = new ProcessBlock(ProcessID, PublicKey);
			if(ProcessID!=2) 
			{
				new Thread(new WaitingStartServer()).start();
				System.out.println("waiting Process2 to send a starting up signal.");
				//waiting for Process 2 to send a starting up signal.
				while(!start)
				{
					Thread.sleep(200);
				}
			}
			else {
				//process2 triggers Process0 and Process1 to start
				System.out.println("Process 2 is sending startup signal to Process 0 and Process 1");
				Socket sock = new Socket(defaultServer, Ports.StartSignalPortBase + (0 * 1000));
				PrintStream out = new PrintStream(sock.getOutputStream());
				out.println("starting up!");
				out.flush();
				sock.close();
				
				sock = new Socket(defaultServer, Ports.StartSignalPortBase + (1 * 1000));
				out = new PrintStream(sock.getOutputStream());
				out.println("starting up!");
				out.flush();
				sock.close();
			} 
			//Start publicKey server
			new Thread(new PublicKeyServer()).start();
			//Start UnverifiedBlockServer
			new Thread(new UnverifiedBlockServer(queue)).start();
			//Start BlockchainServer
			new Thread(new BlockchainServer()).start();
			//waiting the servers to be up and running
			try{Thread.sleep(1000);}catch(Exception e){System.out.println(e.getMessage());} 
			//Read input file and send all unverified blocks to everyone
			new Blockchain().Multicast(); 
			try{Thread.sleep(1000);}catch(Exception e){System.out.println(e.getMessage());} 
			
			//while(!queue.isEmpty()){
			//	System.out.println(queue.take().CreatedTimestamp);
			//}
			//Start to Read Unverified block queue
			new Thread(new UnverifiedBlockConsumer(queue)).start(); 
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void Multicast(){
		 Socket sock;
	     PrintStream out;
	      //multicast public key to other servers.
	      try{
		      for(int i=0; i< numProcesses; i++){
		    	  //there's no need to send the key to the current server itself
		    	  if(i!=ProcessID) {
		    		sock = new Socket(defaultServer, Ports.KeyServerPortBase + (i * 1000));
		    		out = new PrintStream(sock.getOutputStream());
		    		out.println(Blockchain.ProcessID + "`" + BlockchainUtil.PublicKeyToString(PublicKey));
		    		out.flush();
			  		sock.close();
		    	  }
	      } 
	      Thread.sleep(1000); // wait one second for keys to be received.
	      //Read relevant files according to the current ProcessID;
	      List<BlockRecord> blocks = BlockchainUtil.ReadBlocksFromFile(Blockchain.ProcessID);
	      //multicast the unverified blockrecords
	      for(int i=0;i<blocks.size();i++)
	      {
	    	  for(int j=0; j< numProcesses; j++){// Send a real unverified block to each server
	    		//there's no need to send the key to the current server itself
    			sock = new Socket(defaultServer, Ports.UnverifiedBlockServerPortBase + (j * 1000));
  				out = new PrintStream(sock.getOutputStream());
  				//blockrecord json format will be multicast to other servers
  				out.println(blocks.get(i).toString());
  				out.flush();
  				sock.close();
			  }
	      }
	    }catch (Exception ex) 
	      {
	    	ex.printStackTrace();
	      }
	  }
	//defined as a synchronized method for thread safety concerns
	public static synchronized void testAndSet(BlockRecord br) {
		//If the received blockrecord isn't in the blockchain list, then add it to the list
	    Stream<BlockRecord> stream=Blockchain.list.stream();
	    //put this in a synchronized block for thread safety concerns
    	if(stream.filter(c->c.BlockID.equals(br.BlockID)).count()==0)
  	  	  Blockchain.list.add(br);
  	    else{
  	    	//handle cases of blockID collisions and same verifiedtimestamp
  	    	int size=Blockchain.list.size();
  	    	BlockRecord b=Blockchain.list.get(size-1);
  	    	if(b.VerifiedTimestamp.compareTo(br.VerifiedTimestamp)>0)
  	    		Blockchain.list.set(size-1, br);
  	    	else if(b.VerifiedTimestamp.compareTo(br.VerifiedTimestamp)==0) {
  	    		//give priority to processes with smaller process ID
  	    		if(b.VerificationProcessID.compareTo(br.VerificationProcessID)>0)
  	    			Blockchain.list.set(size-1, br);
  	    	}
  	    }
	} 
}

/*
 * waiting to received a blockchain record and test and put in the blockchain list, a potential problem is that if something is
 *   wrong in the process of sending and receiving a blockchain multicast, the blockrecord will not be documented and the whole 
 *   blockchain in out of synch with blockchains in other servers.
 */
class BlockchainServer implements Runnable {
	public void run(){
	  Socket sock;
	  System.out.println("Starting the blockchain server using " + Integer.toString(Ports.BlockchainServerPort));
	  try{
	    ServerSocket servesock = new ServerSocket(Ports.BlockchainServerPort, 6);
	    while (true) {
		sock = servesock.accept();
		//Then ask blockChainWorker to do real job to decide which chain to keep.
		new BlockchainWorker (sock).start(); 
	    }
	  }catch (IOException ioe) {System.out.println(ioe);}
	}
}

//Receive new Blockchain and decide which chain to keep
class BlockchainWorker extends Thread {
	Gson gs=new Gson();
	Socket sock; 
	BlockchainWorker (Socket s) {sock = s;} 
	public void run(){
	  try{
	    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	    String data = in.readLine();
	    BlockRecord br=BlockchainUtil.ParseBlockRecordFromJson(data);
	    //the the synchronized method test and set
	    Blockchain.testAndSet(br);
	    //turn the current blockchain list to json string and write it to the file "BlockchainLedger.json"
	    if(Blockchain.ProcessID == 0)
	  	  BlockchainUtil.WriteBlockLegerToFile(gs.toJson(Blockchain.list), "BlockchainLedger.json");
	    
	    System.out.println("NOW THE NEW BLOCKCHAIN IS: \n" + gs.toJson(Blockchain.list) + "\n\n");
	    sock.close(); 
	  } catch (Exception x){x.printStackTrace();}
	}
}

//add all received unverified blockrecord to a priority queue
class UnverifiedBlockServer implements Runnable {
	BlockingQueue<BlockRecord> queue;
	UnverifiedBlockServer(BlockingQueue<BlockRecord> queue){
	  this.queue = queue;
	}
	public void run(){
	  int q_len = 6;
	  Socket sock;
	  System.out.println("Starting the Unverified Block Server input thread using " + Integer.toString(Ports.UnverifiedBlockServerPort));
	  try{
	    ServerSocket serverSocket = new ServerSocket(Ports.UnverifiedBlockServerPort, q_len);
	    while (true) {
		sock = serverSocket.accept();
		new UnverifiedBlockWorker(sock).start();
	    }
	  }catch (IOException ioe) {System.out.println(ioe);}
	}
	
	class UnverifiedBlockWorker extends Thread {
		  Socket sock;
		  UnverifiedBlockWorker (Socket s) {sock = s;}
		  public void run(){
		    try{
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		    //json format data of BlockRecord and it's in one line
		    String jsondata = in.readLine();
		    BlockRecord br = BlockchainUtil.ParseBlockRecordFromJson(jsondata);
		    System.out.println("Put in priority queue: " + jsondata + "\n");
		    //add data into the priority queue, they will be sorted in terms of createdTimeStamp
		    queue.put(br);
			sock.close(); 
		    } catch (Exception x){x.printStackTrace();}
		  }
		}
}

//waiting for startup signal from process 2
class WaitingStartServer implements Runnable {
		public void run(){
		System.out.println("Starting WaitStartActionServer thread using " + Integer.toString(Ports.StartSignalPort));
		try
		{
		  Socket sock = new ServerSocket(Ports.StartSignalPort).accept();
		  Blockchain.start = true;
		  System.out.println("startup signal received!");
		  sock.close();
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
}

class PublicKeyServer implements Runnable{
	 public static ProcessBlock[] PBlocks = new ProcessBlock[3];
	 
	 public void run(){
	    int q_len = 6;
	    System.out.println("Starting Key Server input thread using " + Integer.toString(Ports.KeyServerPort));
	    try{
	      ServerSocket serverSocket = new ServerSocket(Ports.KeyServerPort, q_len);
	      while (true) {
	    	  Socket sock = serverSocket.accept();
	    	  //Actual parsing and saving work of PublicKey will be done by PublicKeyWorker
	    	  new PublicKeyWorker(sock).start();
	      }
	    }catch (IOException ioe) {System.out.println(ioe);}
	 }
}

//This class is to receive PublicKey and it's corresponding ProcessID and save them to memory
//So that we can use public key to verify blocks
class PublicKeyWorker extends Thread {
	Socket sock;
	PublicKeyWorker (Socket s) {sock = s;}
	public void run(){
	  try{
	    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	    String data = in.readLine();
	    System.out.println("Got Public Key BroadCast Information : " + data);
	    
	    //the data is formatted as:"ProcessID`PublicKey"
	    String[] parameters = data.split("`");
	    if(parameters.length == 2)
	    {
	  	  String processIdStr = parameters[0];
	  	  //get the public key
	  	  String publicKeyStr = parameters[1];
	  	  //get processID
	  	  int processID = Integer.parseInt(processIdStr);
	  	  //store the ProcessID and PublicKey in the memory
	  	  if(PublicKeyServer.PBlocks[processID] == null)
	  	  {
	  		  PublicKeyServer.PBlocks[processID] = new ProcessBlock(processID, BlockchainUtil.StringToPublicKey(publicKeyStr));
	  		  System.out.println("Save Public key and ProcessID to PublicKeyServer.PBlocks: " + processIdStr + " , " + publicKeyStr);
	  	  }
	    }
	    sock.close(); 
	  } catch (Exception e){e.printStackTrace();}
	}
}


class ProcessBlock
{
	//ProcessBlock(){}
	public ProcessBlock(int pid, PublicKey key) {
		processID=pid;
		pubKey=key;
	}
	int processID;
	PublicKey pubKey;
	int port;
	String IPAddress;
}

class Ports{
	  public static int StartSignalPortBase = 6053;
	  public static int KeyServerPortBase = 6050;
	  public static int UnverifiedBlockServerPortBase = 6051;
	  public static int BlockchainServerPortBase = 6052;
	  public static int StartSignalPort;
	  public static int KeyServerPort;
	  public static int UnverifiedBlockServerPort;
	  public static int BlockchainServerPort;
	  Ports(int pid){
		    KeyServerPort = KeyServerPortBase + pid * 1000;
		    UnverifiedBlockServerPort = UnverifiedBlockServerPortBase + pid * 1000;
		    BlockchainServerPort = BlockchainServerPortBase + pid * 1000;
		    StartSignalPort = StartSignalPortBase + pid * 1000;
	  }
}


class BlockchainUtil{
	  static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
	  static Gson gson=new Gson();
	  public static KeyPair generateKeyPair() throws Exception {
	    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
	    kpg.initialize(1024);
	    KeyPair kp=kpg.generateKeyPair();
	    return kp;
	  }
	
	  //decode string back to publicKey
	  public static PublicKey StringToPublicKey(String key) throws Exception {
        byte[] bytes;
        bytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
      }
	  
	  //encode publicKey to base64 string
	  public static String PublicKeyToString(PublicKey publicKey)
	  {
		  byte[] keyByte = publicKey.getEncoded();
		  //convert byte array to base64 String 
		  String keyString = Base64.getEncoder().encodeToString(keyByte);
		  return keyString;
	  }
	  
	//According to input: 0,1,2, read file and create blocks and return them
	  public static List<BlockRecord> ReadBlocksFromFile(int choice) throws Exception
	  {		  
		  String file = "BlockInput"+choice+".txt";
		  List<BlockRecord> list = new ArrayList<>();
		  System.out.println("read from " + file);
		    try {
		    	BufferedReader br = new BufferedReader(new FileReader(file));
				String[] arr = new String[10];
				String input;
			    String suuid;
				while ((input = br.readLine()) != null) {
				  BlockRecord b = new BlockRecord();
				  suuid = UUID.randomUUID().toString();
				  b.setBlockID(suuid);
				  b.setCreatingProcess(choice+"");
				  b.setSignedBlockID(Sign(suuid,Blockchain.PrivateKey));
				  b.setCreatedTimestamp(format.format(new Date()));
				  b.setCreatingProcess("Process" + Integer.toString(choice));
				  b.setVerificationProcessID("-1");
				  b.CreatingProcess = Integer.toString(choice);
				  // split the input into string arrays
				  arr = input.split(" +"); 
				  b.setFname(arr[0]);
				  b.setLname(arr[1]);
				  b.setDOB(arr[2]);
				  b.setSSNum(arr[3]);
				  b.setDiag(arr[4]);
				  b.setTreat(arr[5]);
				  b.setRx(arr[6]);
				  list.add(b);
				}
				System.out.println(list.size() + " records read, names:");
				for(int i=0; i < list.size(); i++){
				  System.out.println(list.get(i).getFname() + " " + list.get(i).getLname());
				}
		    }catch(Exception ex) {
		    	ex.printStackTrace();
		    }
		    //use SHA256 and SignedSHA256 as the digital signature
		    for(BlockRecord br : list)
		    {
		    	String json = br.toString();
		    	String sHA256String=getSHA256(json);
		    	byte[] digitalSignature = signData(sHA256String.getBytes(), Blockchain.PrivateKey);
		    	String signedSHA256=Base64.getEncoder().encodeToString(digitalSignature);
		    	br.setSHA256String(sHA256String);
		    	br.setSignedSHA256(signedSHA256);
		    }
		  return list;
	  }
	  
	  //Convert json string to BlockRecord object
	  public static BlockRecord ParseBlockRecordFromJson(String blockActualJsonData)
	  {
		BlockRecord blockRecord = gson.fromJson(blockActualJsonData, BlockRecord.class);
		return blockRecord;
	  }
	  
      //Write blockchain to file
	  public static void WriteBlockLegerToFile(String content,String path) throws IOException
	  {
		  File file = new File(path);
		  if(!file.exists())
		  {
			file.createNewFile();
		  }
		  else
		  {
			  file.delete();
			  file.createNewFile();
		  }
		  FileWriter fileWriter = new FileWriter(path);
		  PrintWriter printWriter = new PrintWriter(fileWriter);
		  printWriter.print(content);
		  printWriter.close();
	  }
	  
	  //convert block object to json string
	  public static String BlockRecordToJson(BlockRecord blockRecord)
	  {
		  String brJson=gson.toJson(blockRecord);
		  return brJson;
	  }
		public static String getSHA256(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
			MessageDigest messageDigest;
			String encodestr = "";
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(str.getBytes("UTF-8"));
			encodestr = byte2Hex(messageDigest.digest());
			return encodestr;
		}
		
		private static String byte2Hex(byte[] bytes) {
			StringBuffer sb = new StringBuffer();
			String temp = null;
			for (int i = 0; i < bytes.length; i++) {
				temp = Integer.toHexString(bytes[i] & 0xFF);
				if (temp.length() == 1) {
					// prepend a zero if the length is 1
					temp="0"+temp;
				}
				sb.append(temp);
			}
			return sb.toString();
		}

		public static boolean signitureVerify(byte[] data, PublicKey key, byte[] sig) throws Exception {
		    Signature signer = Signature.getInstance("SHA1withRSA");
		    signer.initVerify(key);
		    signer.update(data);
		    return (signer.verify(sig));
	  }
	  
	  public static String Sign(String input,PrivateKey privateKey) throws Exception
	  {
    	  byte[] digitalSignature = signData(input.getBytes(),privateKey);
    	  return Base64.getEncoder().encodeToString(digitalSignature);
	  }
	  
	  public static byte[] signData(byte[] data, PrivateKey key) throws Exception 
	  {
	    Signature signer = Signature.getInstance("SHA1withRSA");
	    signer.initSign(key);
	    signer.update(data);
	    return (signer.sign());
	  }
}

//This class will consume blockrecord from the block queue
class UnverifiedBlockConsumer implements Runnable {
	BlockingQueue<BlockRecord> queue;
	int ProcessID;
	UnverifiedBlockConsumer(BlockingQueue<BlockRecord> queue){this.queue = queue;}
	
	public void run(){
	  PrintStream out;
	  System.out.println("Starting the Unverified Block Priority Queue Consumer thread.\n");
	  try{
	    while(true){ 
	    	//will wait for available resources if the queue is empty
	    	BlockRecord blockRecord = queue.take(); 
			System.out.println("Got unverified block: " + blockRecord);
			System.out.println("Start verifying this block...");
			
			int pid = Integer.parseInt(blockRecord.CreatingProcess);
			 //verify the blockrecord before moving forward
			boolean verified=BlockchainUtil.signitureVerify(blockRecord.SHA256String.getBytes(),
		    				PublicKeyServer.PBlocks[pid].pubKey,
		    				Base64.getDecoder().decode(blockRecord.SignedSHA256));
			// if not verified, skip the current verification and work
			if(!verified)
				continue;
			//if verified, continue and do the work
			//the work is done about previous hashcode, blockID of the blockRecord and a random string (will be generated in the BCWorker)
			String previousBlockHash=GetPreviousBlockHash(Blockchain.list);
			String hashcode = BCWorker.DoWork(blockRecord.BlockID, previousBlockHash + blockRecord.BlockID);
		    if(hashcode != null)
		    {
		    	//doesn't mean this blockrecord is guarantee be added to the canonical blockchain, still need to compare verifiedtimestamp and processid
		    	System.out.println("solved the puzzle! MultiCasting a new blockrecord!");
		    	blockRecord.setVerifiedTimestamp(BlockchainUtil.format.format(new Date()));
		    	blockRecord.setHashCode(hashcode);
		    	blockRecord.setPreviousHash(previousBlockHash);
		    	blockRecord.VerificationProcessID = Integer.toString(Blockchain.ProcessID);
				BlockRecord verifiedBlock = blockRecord;
				Blockchain.list.add(verifiedBlock);
			    for(int i=0; i < Blockchain.numProcesses; i++)
			    {
			    	Socket sock = new Socket(Blockchain.defaultServer, Ports.BlockchainServerPortBase + (i * 1000));
				    out = new PrintStream(sock.getOutputStream());
				    out.println(verifiedBlock);
				    out.flush();
				    sock.close();
			    }
		    }
		    //wait 1.5 second to make sure the multicast is complete!
			Thread.sleep(1500);
	    }
	  }catch (Exception e) 
	  {
		  e.printStackTrace();
	  }
	}
	
	public static String GetPreviousBlockHash(List<BlockRecord> list)
	{
		  if(list.size()==0)
		  {
			  //If the block Chain list has no elements, return a fake HashCode.
			  return "this is the first block chain";
		  }
		  else
		  {
			  //Otherwise return the previous hashcode
			  String previousHashCode =list.get(list.size()-1).HashCode;
			  System.out.println("The previous hashcode is "+ previousHashCode);
			  return previousHashCode;
		  }
	}
}

//This class will spend some time to calculate hashcode that meets certain requirement
class BCWorker {
	 static Gson gson=new Gson();
	 static Random r=new Random();
	 
	 //Get a 5-character random string in smaller cases
	 public static String randomString() {
		 StringBuilder sb=new StringBuilder();
		 for(int i=0;i<5;i++)
			 sb.append((char)(r.nextInt(26)+'a'));
		    return sb.toString();
		}
	
	//Do some time spending time and return a hashcode if puzzle is solved,otherwise return null
	public static String DoWork(String unverifiedBlockId, String in)
	{
		    String cat = "";
		    String out = "";
		    String randString = "";
		    int testNumber = 0;
		    try {
		        while(true)
		        {
					randString = randomString();
					//reset the cat
					cat="";
					cat = in + randString;
					MessageDigest MD = MessageDigest.getInstance("SHA-256");
					byte[] bytesHash = MD.digest(cat.getBytes("UTF-8"));
					out = DatatypeConverter.printHexBinary(bytesHash);
					
					testNumber = Integer.parseInt(out.substring(0,4),16);
					System.out.println("First 16 bits " + out.substring(0,4) +": " + testNumber + "\n");
					if (testNumber < 5000){
					  System.out.println("the puzzle solved!");
					  System.out.println("the random string is: " + randString);
					  return out;
					}
					Thread.sleep(200 + new Random().nextInt(400));
					if(Blockchain.list.stream().filter(c->c.BlockID.equals(unverifiedBlockId)).count()!=0)
					{
						return null;
					}
				}
		    }catch(Exception ex) {ex.printStackTrace();}
		    
		    return null;
	}
}
class BlockRecord implements Comparable<BlockRecord>{
	  String BlockID;
	  String SHA256String;
	  String SignedSHA256;
	  String SignedBlockID;
	  String HashCode;
	  String PreviousHash;
	  String CreatedTimestamp;
	  String VerifiedTimestamp;
	  //this mark works as a reward
	  String VerificationProcessID;
	  String CreatingProcess;
	  String Fname;
	  String Lname;
	  String SSNum;
	  String DOB;
	  String Diag;
	  String Treat;
	  String Rx;
	  
	public String getSHA256String() {
		return SHA256String;
	}
	public void setSHA256String(String sHA256String) {
		SHA256String = sHA256String;
	}
	public String getSignedSHA256() {
		return SignedSHA256;
	}
	public void setSignedSHA256(String signedSHA256) {
		SignedSHA256 = signedSHA256;
	}
	public String getBlockID() {
		return BlockID;
	}
	public void setBlockID(String blockID) {
		BlockID = blockID;
	}
	public String getSignedBlockID() {
		return SignedBlockID;
	}
	public void setSignedBlockID(String signedBlockID) {
		SignedBlockID = signedBlockID;
	}
	public String getHashCode() {
		return HashCode;
	}
	public void setHashCode(String hashCode) {
		HashCode = hashCode;
	}
	public String getCreatedTimestamp() {
		return CreatedTimestamp;
	}
	public void setCreatedTimestamp(String createdTimestamp) {
		CreatedTimestamp = createdTimestamp;
	}
	public String getVerifiedTimestamp() {
		return VerifiedTimestamp;
	}
	public void setVerifiedTimestamp(String verifiedTimestamp) {
		VerifiedTimestamp = verifiedTimestamp;
	}
	public String getVerificationProcessID() {
		return VerificationProcessID;
	}
	public void setVerificationProcessID(String verificationProcessID) {
		VerificationProcessID = verificationProcessID;
	}
	public String getCreatingProcess() {
		return CreatingProcess;
	}
	public void setCreatingProcess(String creatingProcess) {
		CreatingProcess = creatingProcess;
	}
	public String getPreviousHash() {
		return PreviousHash;
	}
	public void setPreviousHash(String previousHash) {
		PreviousHash = previousHash;
	}
	public String getFname() {
		return Fname;
	}
	public void setFname(String fname) {
		Fname = fname;
	}
	public String getLname() {
		return Lname;
	}
	public void setLname(String lname) {
		Lname = lname;
	}
	public String getSSNum() {
		return SSNum;
	}
	public void setSSNum(String sSNum) {
		SSNum = sSNum;
	}
	public String getDOB() {
		return DOB;
	}
	public void setDOB(String dOB) {
		DOB = dOB;
	}
	public String getDiag() {
		return Diag;
	}
	public void setDiag(String diag) {
		Diag = diag;
	}
	public String getTreat() {
		return Treat;
	}
	public void setTreat(String treat) {
		Treat = treat;
	}
	public String getRx() {
		return Rx;
	}
	public void setRx(String rx) {
		Rx = rx;
	}
	@Override
	public int compareTo(BlockRecord o) {
		return this.CreatedTimestamp.compareTo(o.CreatedTimestamp);
	}
	//return the json format of the instance
	@Override
	public String toString() {
		return BlockchainUtil.BlockRecordToJson(this);
	}
}
