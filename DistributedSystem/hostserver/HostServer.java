

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * HostServer Notes: This is a relatively easy assignment, but I did learn something from, including how to store server 
 * state and how to stop listening on one port and start a new port handling future request.
 */
class AgentWorker extends Thread {
	Socket sock; //socket connection to handle client request
	agentHolder parentAgentHolder; //maintains state information, stores current running socket object and current state
	int localPort; //port being used by this request
	//constructor
	AgentWorker (Socket s, int prt, agentHolder ah) {
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() {
		//set initial value as null
		PrintStream out = null;
		BufferedReader in = null;
		//use "localhost" as default server
		String NewHost = "localhost";
		//use "1565" as the default port
		int NewHostMainPort = 1565;		
		String buf = "";
		int newPort;
		Socket clientSock;
		BufferedReader fromHostServer;
		PrintStream toHostServer;
		
		try {
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//read a line string from the client
			String inLine = in.readLine();
			//use a stringbuilder to assemble html response to the client
			StringBuilder htmlString = new StringBuilder();
			
			//print the request on the console
			System.out.println();
			System.out.println("Request line: " + inLine);
			
			//detect the "migrate" key word
			if(inLine.indexOf("migrate") > -1) {
				//create a new socket with the main server waiting on 1565
				clientSock = new Socket(NewHost, NewHostMainPort);
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				//send a request to port 1565 to receive the next open port
				toHostServer = new PrintStream(clientSock.getOutputStream());
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
				toHostServer.flush();
				
				//wait for the response and read a response until we find what should be a port
				for(;;) {
					//read the line and check it for what looks to be a valid port
					buf = fromHostServer.readLine();
					if(buf.indexOf("[Port=") > -1) {
						break;
					}
				}
				//parse port data 
				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );
				//parse the response for the integer containing the new port
				newPort = Integer.parseInt(tempbuf);
				//print the new port on the server console
				System.out.println("newPort is: " + newPort);
				
				//prepare the response string to send to the client
				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
				//inform the user that the migration request was received
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
				//finish html
				htmlString.append(AgentListener.sendHTMLsubmit());

				//show in the console that we are killing the waiting server at the port, a socket closed exception will be thrown
				System.out.println("Killing parent listening loop.");
				//stopping listening at previous port held by the parentAgentHolder
				parentAgentHolder.sock.close();
				
				
			} else if(inLine.indexOf("person") > -1) {
				//increase the state variable in the AgentHolder 
				parentAgentHolder.agentState++;
				//send the html back to the user displaying the agent state and form
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				//we couldnt find a person variable, so we probably are looking at a fav.ico request
				//tell the user it was invalid
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());		
				
		
			}
			//reponse with the htmlString
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);
			
			//close the socket
			sock.close();
			
			
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
	
}
/**
 * this class is used to track the state information and socket object
 */
class agentHolder {
	//active socket object
	ServerSocket sock;
	//use a int to present state information, can be very complicated in real-world application
	int agentState;
	
	//constructor
	agentHolder(ServerSocket s) { sock = s;}
}
/**
 * AgentListener objects watch individual ports and respond to requests
 * made upon them(in this scenario from a standard web browser); Craeted 
 * by the hostserver when a new request is made to the current port
 *
 */
class AgentListener extends Thread {
	//global varibles for AgentLister
	Socket sock;
	int localPort;
	
	//constructor of the class
	AgentListener(Socket As, int prt) {
		sock = As;
		localPort = prt;
	}
	//let the default agent state be 0
	int agentState = 0;
	
	//called when thread.start() is called
	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String NewHost = "localhost";
		System.out.println("In AgentListener Thread");		
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));
			//read first line
			buf = in.readLine();
			//if we have a state key word, parse the request and store it
			if(buf != null && buf.indexOf("[State=") > -1) {
				//extract the state from the read line
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));
				//parse the string as an integer
				agentState = Integer.parseInt(tempbuf);
				//print it on the server console
				System.out.println("agentState is: " + agentState);
			}
			System.out.println(buf);
			//string builder to hold the html response
			StringBuilder htmlResponse = new StringBuilder();
			//output first request html to user
			//show the port and display the form. we know agentstate is 0 since game hasnt been started
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());
			//display it
			sendHTMLtoStream(htmlResponse.toString(), out);
			
			//now open a connection at the port
			ServerSocket servsock = new ServerSocket(localPort,2);
			//create a new agentholder and store the socket and agentState
			agentHolder agenthold = new agentHolder(servsock);
			agenthold.agentState = agentState;
			
			//listening to new request.
			while(true) {
				sock = servsock.accept();
				//log a received connection
				System.out.println("Got a connection to agent at port " + localPort);
				//connection received. create new agentworker object and start it up!
				new AgentWorker(sock, localPort, agenthold).start();
			}
		
		} catch(IOException ioe) {
			//exception handling
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}
	//set html response, which will be displayed in the browser
	//add port to action attribute so the next request goes back to the latest updated port
	static String sendHTMLheader(int localPort, String NewHost, String inLine) {
		
		StringBuilder htmlString = new StringBuilder();
		htmlString.append("<html><head> </head><body>\n");
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		//NewHost and localPort are changed each time the user type in "migrate", the change will be reflected with next request
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");
		
		return htmlString.toString();
	}
	//last part of the html string, submit button
	static String sendHTMLsubmit() {
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}
	//send the response headers and calculate the content length so we play nicer with all browsers,
	//work well with both IE and non ie browser
	static void sendHTMLtoStream(String html, PrintStream out) {
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html);
	}
	
}
/**
 * 
 * hostserver class. initially listens on port 1565 for requests. 
 * for each "migrate" command, stop listening on the previous port and starts listening on the new port
 */
public class HostServer {
	//base port
	public static int NextPort = 3000;
	
	public static void main(String[] a) throws IOException {
		int q_len = 6;
		int port = 1565;
		Socket sock;
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("John Reagan's DIA Master receiver started at port 1565.");
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");
		//listen on port 1565 for new requests OR migrate requests
		while(true) {
			//increase the port number, can be complicated in real-world application
			NextPort = NextPort + 1;
			//create a new socket for to handle a new requests
			sock = servsock.accept();
			//print the current port info
			System.out.println("Starting AgentListener at port " + NextPort);
			//create new agent listener at this port to wait for requests
			new AgentListener(sock, NextPort).start();
		}
		
	}
}
