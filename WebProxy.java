/**
 * WebProxy
 *
 * @author      Matthew Hylton
 * @version     1.0, 3 Feb 2017
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.io.InputStream;

class Request{
  private String method;
  private String url;
  private String version;
  private String hostname;
  private byte[] body;
  private ArrayList<String> headers;

  /**
  *  Constructor that initalizes the client Requests
  *
  * @param method       client request method
  * @param url          client request url
  * @param version      client request version
  * @param hostname     client request hostname
  * @param headers      client request headers
  */

  public Request(String method, String url, String version, String hostname, ArrayList<String> headers)
  {
    /* Intialize request variables */
    this.method = method;
    this.url = url;
    this.version = version;
    this.hostname = hostname;
    this.headers = headers;
  }

  public String getMethod() {return method;}
  public String getUrl() {return url;}
  public String getVersion() {return version;}
  public String getHostname() {return hostname;}
  public byte[] getBody() {return body;}
  public ArrayList<String> getHeaders() {return headers;}

  public void setMethod(String method) {this.method = method;}
  public void setUrl(String url) {this.url = url;}
  public void setVersion(String version) {this.version = version;}
  public void setHostname(String hostname) {this.hostname = hostname;}
  public void setBody(byte[] body) {this.body = body;}
  public void setHeaders(ArrayList<String> headers) {this.headers = headers;}
}

public class WebProxy {

  public int proxyPort;

  /**
  *  Constructor that initalizes the server listening port
  *
  * @param port      Proxy server listening port
  */

	public WebProxy(int port) {

	/* Intialize server listening port */
  proxyPort = port;

	}

	public void start(){

    // Request parameters
    String method;
    String url;
    String version;
    String header;
    String hostname;
    ArrayList<String> headers = new ArrayList<String>();

    byte bodyData;
    int length = 262144;
    byte[] body = new byte[length];
    String sbody;

    // Local cache
    HashMap<String, Request> cache = new HashMap<String, Request>();

    //Server parameters
    String request;
    ServerSocket serverSocket = null;
		Socket socket = null;

    //Connect to browser
    try {
      serverSocket = new ServerSocket(proxyPort);
      while(true) {
        socket = serverSocket.accept();

        //Create necessary streams
        OutputStream outputStream = socket.getOutputStream();
        Scanner inputStream = new Scanner(socket.getInputStream());

        //Get request and parse it
        request = inputStream.nextLine();
        method = request.split(" ")[0];
        if (method.equals("GET")) {
          url = request.split(" ")[1];
          version = request.split(" ")[2];
          hostname = url.split("://")[1].split("/")[0];
          header = inputStream.nextLine();
          while(!header.isEmpty()) {
            headers.add(header);
            header = inputStream.nextLine();
          }

          //Store the request
          Request httpRequest = new Request(method, url, version, hostname, headers);

          //Check for request in cache
          if (cache.containsKey(httpRequest.getUrl())) { //service from cache

            //Retrive from cache
            outputStream.write(cache.get(httpRequest.getUrl()).getBody());
            outputStream.flush();

          } else { //Retrive from web

            //Connect to server
            try{
              socket = new Socket(httpRequest.getHostname(), 80);

              //Create necessary streams
              PrintWriter weboutputStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
              InputStream webinputStream = socket.getInputStream();

              //Send request
              weboutputStream.println(httpRequest.getMethod() + " " + httpRequest.getUrl() + " " + httpRequest.getVersion());
              for (int i = 0; i < httpRequest.getHeaders().size(); i++) {
                weboutputStream.println(httpRequest.getHeaders().get(i));
              }
              weboutputStream.println();
              weboutputStream.flush();

              //Recive response
              for (int j = 0 ;j < length ; j++) {
                body[j] = (byte) webinputStream.read();
              }

              sbody = new String(body);

              //Check responce
              if (sbody.split("\n")[0].split(" ")[1].equals("200")) {
                //Send response
                outputStream.write(body);
                outputStream.flush();

                //Save to cache
                httpRequest.setBody(body);
                cache.put(httpRequest.getUrl(), httpRequest);
              } else {
                System.out.println("400 Bad Request");
                String error = "400 Bad Request";
                outputStream.write(error.getBytes());
                outputStream.flush();
              }

            } catch (Exception e) {
          		System.out.println("Error: " + e.getMessage());
          	}
          }
        } else {
          System.out.println("400 Bad Request");
          String error = "400 Bad Request";
          outputStream.write(error.getBytes());
          outputStream.flush();
        }
      }
    }


    catch (Exception e){
      System.out.println("Error: " + e.getMessage());
    }
    finally {

      if (socket != null) {
        try {
          socket.close();
        }
        catch (IOException ex) {}
      }

      if (serverSocket != null) {
        try {
          serverSocket.close();
        }
        catch (IOException ex) {}
      }
    }
	}

	public static void main(String[] args) {

    String server = "localhost"; // webproxy and client runs in the same machine
    int server_port = 0;

      try {
        // check for command line arguments
        if (args.length == 1) {
          server_port = Integer.parseInt(args[0]);
        } else {
          System.out.println("wrong number of arguments, try again.");
          System.out.println("usage: java WebProxy port");
          System.exit(0);
        }

        WebProxy proxy = new WebProxy(server_port);

        System.out.printf("Proxy server started...\n");
        proxy.start();
      } catch (Exception e) {
			  System.out.println("Exception in main: " + e.getMessage());
        e.printStackTrace();
		  }
	}
}
