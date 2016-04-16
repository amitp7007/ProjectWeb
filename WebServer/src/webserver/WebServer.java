package webserver;

import in2011.http.MessageFormatException;
import in2011.http.Request;
import in2011.http.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class WebServer {
	public enum HttpMethods{
		GET, POST, PUT, TRACE, DELETE
	}

    private int port;
    private String rootDir;
    ExecutorService executor = Executors.newFixedThreadPool(200);
	//added by me
	public class RequestHandler implements Runnable{
		Socket socket = null;
		
		public RequestHandler(Socket socket2) {
			socket = socket2;
		}

		private void processDataRequest(InputStream is, OutputStream outstr){
			try {
				Response response = new Response(200);
				int noOfTry = 2;
				while(true){
					if(is.available()==0 && noOfTry-- > 0){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else{
						break;
					}
				}
				if(is.available()==0) return;
				Request request = null;
				try{
					 request = Request.parse(is);
				}
				catch(MessageFormatException e){
					response = new Response(400);
					response.write(outstr);
					outstr.flush();
					return;
				}
				String url = request.getURI();
				String methodName = request.getMethod();
				String contentType = request.getHeaderFieldValue("Content-Type");
				response.addHeaderField("Content-Type", contentType);
				
				String resourcePath =  rootDir + "/" + url;
				Path absolutePath = Paths.get(resourcePath).toAbsolutePath().normalize();
				System.out.println("Aboslour Path " + absolutePath);
				String uri = URLDecoder.decode(url, "ASCII");
				HttpMethods method = HttpMethods.valueOf(methodName);
				switch(method){
				case GET:{
					try{
						response = new Response(200);
						InputStream fileInputStream = Files.newInputStream(absolutePath);
						byte[] a = new byte[4096];
				        int n;
				        response.write(outstr);
				        while ((n=fileInputStream.read(a))>0){
				          outstr.write(a, 0, n);
				        }
				        outstr.flush();
					}
					catch(IOException e){
						response = new Response(404);
						response.write(outstr);
						
						outstr.flush();
						System.out.println(e);
					}
	
				}
					break;
				case POST:
					break;
				default:
					break;
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Response response = new Response(404);
				try {
					response.write(outstr);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		
		}
		public void run(){
			InputStream inStr = null;
	    	OutputStream outStr = null;
			try {
				
				inStr = socket.getInputStream();
				
				outStr = socket.getOutputStream();
				//processing request
				processDataRequest(inStr, outStr);
				
		        System.out.println("Request Recieved : Sending Response : " );
		        
		        outStr.flush();
		       // socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				try {
					inStr.close();
					outStr.close();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}
	}
	

    public WebServer(int port, String rootDir) {
        this.port = port;
        this.rootDir = rootDir;
    }

    public void start() throws IOException {
        ServerSocket socket = new ServerSocket(this.port);
        System.out.println("Server Started : port + " + this.port + " : " + this.rootDir);
        while(true){
        	Socket responseCl = socket.accept();
        	Runnable reHandler = new RequestHandler(responseCl);
            executor.execute(reHandler);
        }
    }

    public static void main(String[] args) throws IOException {
        String usage = "Usage: java webserver.WebServer <port-number> <root-dir>";
        if (args.length != 2) {
            throw new Error(usage);
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        String rootDir = args[1];
        WebServer server = new WebServer(port, rootDir);
        server.start();
    }
}
