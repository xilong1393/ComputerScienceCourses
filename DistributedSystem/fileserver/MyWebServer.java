import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyWebServer {

	//relative directory
	static String serverDirectory="";
	//format the time
	static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss  ");

	public static void main(String[] args)
    {
		try {
			initialize();
	        int port =2540; //set the port to be 2540
	        //String serverDirectory = args[1];
			serverDirectory =  new File("").getAbsolutePath();
	        // open server socket
	        ServerSocket socket   = new ServerSocket(port); 
	        System.out.println("My webserver started at port " + port);
	        // waiting for the new request
	        while (true) {
	            try {
	                // captured a new request, start a new socket to handle it
	            	Socket s = socket.accept();
	                new Worker(s).run(); 
	                }catch(Exception ex) {
	                	System.err.println(ex.getMessage());
	                }
	            }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
    }
	private static void initialize() throws IOException {
		File f1 = new File(".") ;
		File file=new File(f1,"index.html");
		file.createNewFile();
	    FileWriter myWriter = new FileWriter("index.html");
	    recursive(myWriter, f1);
	    myWriter.close();
	}
	private static void recursive(FileWriter myWriter, File file) throws IOException{
		File[] dirs = file.listFiles();
		if(dirs.length==0) {
			//if the fold empty, return, the browser will automatically convert a back slash to slash in the url
			myWriter.write( "<a href='"+file+"\\'>" +file.toString().substring(2)+ "\\</a><br>\n") ;
			return;
		}
		for ( int i = 0 ; i < dirs.length ; i ++ ) {
	    	if(dirs[i].isDirectory())
	    		//if the current item is a folder, recursively get the file list of the folder
	    		recursive( myWriter, dirs[i]);
	    	else
	    		//if the current item is file, stop recursion and add a link to the index.html
	    	  myWriter.write( "<a href='"+dirs[i]+"'>" + dirs[i].toString().substring(2)+"</a><br>\n") ;
	    }
	}
	//current not using this, might change it later
	/*private static String getFilename(File file) {
		String filename=file.getName();
		int length=file.toString().length();
		StringBuilder sb=new StringBuilder();
		int spacecount=length-filename.length();
		for(int i=0;i<spacecount;i++) {
			sb.append("-");
		}
		return sb.append(filename).toString();
	}*/
}

class Worker extends Thread  {    // Worker class will start a new thread
	  Socket sock;
	  Worker (Socket s) {sock = s;}
	  public void run(){
		  try {
			  //create an buffer reader object to get input from the client
			  BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			  //create an output object to write output to the client
	          OutputStream output = new BufferedOutputStream(sock.getOutputStream());
	          //create a printStream to communicate with the clients
	          PrintStream out = new PrintStream(output);
	          // read first line of request (ignore the rest)
	          String request = input.readLine();
	          if (request==null)
	              return;
	          System.out.println(MyWebServer.format.format(new Date()) +  request);
	          while (true) {
	              String misc = input.readLine();
	              if (misc==null || misc.length()==0)
	                  break;
	          }
	          // analyze the request
	          if (!request.endsWith("HTTP/1.1")||!request.startsWith("GET") ) {
	              // bad request
	        	  responseWithError("Bad Request", "the request format is not correct.",out );
	          } else {
	              String requestInput = request.substring(4, request.length()-9).trim();
	              if (requestInput.endsWith(".class")||requestInput.startsWith("/.")) {
	                 //security checking, .class file and files starts with . is not allowed to be accessed to downloaded
	            	  responseWithError("access denied","you have no permission to access this file",out);
	              } else {
	            	  if(requestInput.contains("?")) {
	            		  String query = requestInput.split("\\?")[1];
	            		  Map<String,String> map=parseQuery(query);
	            		  int num1=Integer.parseInt(map.get("num1"));
	            		  int num2=Integer.parseInt(map.get("num2"));
	            		  int sum=num1+num2;
	            		  String person=map.get("person");
	            		  String message="Dear " +person+", the sum of "+num1+" and "+num2+" is "+sum+"!";
                          //this is the response header
                          out.print("HTTP/1.0 200 OK\r\n" +
                                     "Content-Type: " + "text/html" + "\r\n" +
                                     "time: " + MyWebServer.format.format(new Date())+"\r\n" +
                                     "Server: Xilong Yu's WebServer 1.0\r\n\r\n");
                          // send the response to the client 
                          responseWithHtml("AddNum Result", message, out);
                          System.out.println(MyWebServer.format.format(new Date()) +  "success");
	            	  }
	            	  else {
	            		  String path = MyWebServer.serverDirectory + "/" + requestInput;
		                  File f = new File(path);
	                      if (f.isDirectory()&&requestInput.equals("/")) {
	                          // if directory, add 'index.html' automatically
	                          path = path + "index.html";
	                          System.out.println(path);
	                          f = new File(path);
	                      }
	                      try { 
	                          // send file
	                          InputStream file = new FileInputStream(f);
	                          //this is the header
	                          out.print("HTTP/1.0 200 OK\r\n" +
	                                     "Content-Type: " + getContentType(path) + "\r\n" +
	                                     "time: " + MyWebServer.format.format(new Date())+"\r\n" +
	                                     "Server: Xilong Yu's WebServer 1.0\r\n\r\n");
	                          // send the response to the client 
	                          reponseToClient(file, output); 
	                          System.out.println(MyWebServer.format.format(new Date()) +  "success");
	                      } catch (Exception e) {
	                    	  //show error message on the server machine
	                    	  System.err.println(MyWebServer.format.format(new Date()) + e.toString() + "\r\n");
	                    	  //response to the client with a message
	                          responseWithError("internal error.", e.getMessage(), out);
	                      }
	            	  }
	              }
	          }
	          output.flush();
	    	  sock.close();  
		  }catch(Exception ex) {
			  ex.printStackTrace();
		  }
	  }
	  public static Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
		    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		    String[] pairs = query.split("&");
		    for (String pair : pairs) {
		        int idx = pair.indexOf("=");
		        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		    }
		    return query_pairs;
		}

	   private void responseWithHtml(String title, String message, PrintStream out)
	    {
		   out.print("<!DOCTYPE HTML>\r\n" +
                   "<TITLE>" + title + "</TITLE>\r\n" +
                   "</HEAD><BODY>\r\n" +
                   "<H1>" + message + "</H1>\r\n" +
                   "</BODY></HTML>\r\n");
	    }
	    private void responseWithError(String title, String message, PrintStream out)
	    {
	    	out.print("HTTP/1.0 " +  title + "\r\n" + "\r\n" +
	    			   "<!DOCTYPE HTML>\r\n" +
	                   "<TITLE>" + title + "</TITLE>\r\n" +
	                   "</HEAD><BODY>\r\n" +
	                   "<H1>" + title + "</H1>\r\n" + message + "<P>\r\n" +
	                   "</BODY></HTML>\r\n");
	    }
	    
	    private String getContentType(String path)
	    {
	    	if (path.endsWith(".png") || path.endsWith(".jpg"))
	            return "image/jpeg";
	    	else if (path.endsWith(".txt") || path.endsWith(".java"))
	            return "text/plain";
	    	else if (path.endsWith(".html"))
	            return "text/html";
	        else
	            //return "application/octet-stream";
	        	return "text/plain";
	    }
	    
	    private void reponseToClient(InputStream in, OutputStream out) throws IOException
	    {
    		byte[] buffer = new byte[1024];
    		int len = in.read(buffer);
    		while (len != -1) {
    		    out.write(buffer, 0, len);
    		    len = in.read(buffer);
    		}
	    }
	}
