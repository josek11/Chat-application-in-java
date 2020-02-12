import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
  int port;
  String nome;
  String sala;
  int estado;
  SelectionKey key;
  SocketChannel channel;

  
  public User(int myport, SelectionKey key){
    this.port = myport;
    this.sala = "undef";
    this.nome = "undef";
    this.estado = 0;
    this.key=key;
    this.channel = (SocketChannel)key.channel();
  }
  
  public void setName(String mynome){
    this.nome = mynome;
    this.estado = 1;
  }
  
  public void setRoom(String mysala){
    this.sala = mysala;
    this.estado = 2;
  }
  
  public void leaveRoom(){
    this.sala = "undef";
    this.estado = 1;
  }
}


public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  static private final HashMap<Integer,String> nomes = new HashMap<>();
  static private final HashMap<String,Integer> salas = new HashMap<>();
  static private int i = 0; 
  static private User[] userArray = new User[99];
  static private Selector selector;


  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector2 = Selector.open();
      selector = selector2;

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
            SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );
            
            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );
			
            // Register it with the selector, for reading
            // regista tambem com o numero da porta do socket
            User u = new User(s.getPort(), sc.register( selector, SelectionKey.OP_READ, i));
            userArray[i]= u;
            i++;


          } else if ((key.readyOps() & SelectionKey.OP_READ) ==
            SelectionKey.OP_READ) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              Object object = key.attachment();
              int id = (int) object;
              boolean ok = processInput(id);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput(int id) throws IOException {
    SocketChannel sp = userArray[id].channel;
    buffer.clear();
    sp.read( buffer );
    //modo escrita
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) {
      mandarTodos(userArray[id].nome, "LEFT ", userArray[id].sala, id, 1);
      return false;
    }
    // Decode and print the message to stdout
    String message = decoder.decode(buffer).toString();

    if(message.startsWith("/nick ")){
      criarNick(message.substring(6), id);
    }
    else if(message.startsWith("/join ")){
      String antiga = userArray[id].sala;
      if(userArray[id].estado==2){
        mandarTodos(userArray[id].nome, "LEFT ", antiga, id, 1);
      }
      criarSala(message.substring(6), id);
    }
    else if(message.equals("/leave")){
      leave(id);
    }
    else if(message.equals("/bye")){
      customLeave(id);
    }


    else{
		int myflag=0;
		String especial ="";
		if(message.startsWith("//")){
			especial="MESSAGE "+userArray[id].nome+" "+message.substring(1)+"\n";
			myflag=1;
		}
      String salamsg = userArray[id].sala;
      if(userArray[id].estado==2){
        buffer.clear();
        String message2 = "MESSAGE "+userArray[id].nome+" "+message+"\n";
        if(myflag==0){
        buffer.put(message2.getBytes());}
        else if(myflag==1){
			buffer.put(especial.getBytes());
		}
    	 //metodo que retorna os canais prontos
    	  Set<SelectionKey> selectedKeys = selector.keys();
    	 //iterar nos canais
    	  Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
    	  buffer.flip();
    	 
    	  while(keyIterator.hasNext()){
    		  SelectionKey key = keyIterator.next();

          if(key.isAcceptable())
            continue;
    		 
    		  sp = (SocketChannel)key.channel();
          Socket temp_s = sp.socket();
          int myport = temp_s.getPort();
          Object objeto = key.attachment();
          int teste = (int) objeto;
          if(userArray[teste].sala.equals(salamsg)){
            sp.write(buffer);
            buffer.rewind();
          }
    	  }
        buffer.clear();
        }
    else{
      buffer.clear();
      message = "ERROR\n";
      sp.write(ByteBuffer.wrap(message.getBytes("UTF-8")));
    }
  }

    return true;
  }


  public static void criarNick(String nickname, int id) throws IOException{
    if(!nomes.containsValue(nickname)){
      nomes.put(id, nickname);
      printarErro("OK\n", id);
      if(userArray[id].estado==0){
        userArray[id].setName(nickname);
      }
      if(userArray[id].estado==2){
        mandarTodos(nickname, "NEWNICK "+userArray[id].nome, userArray[id].sala, id, 0);
        userArray[id].nome=nickname;
      }
    }
    else{
      printarErro("ERROR\n", id);

    } 
  }

  public static void criarSala(String sala, int id) throws IOException{
    if(userArray[id].estado==0){
      printarErro("ERROR\n", id);
    }
    else if(!salas.containsKey(sala)){
      salas.put(sala,1);
      printarErro("OK\n", id);
      userArray[id].setRoom(sala);
    }
    else if(salas.containsKey(sala)){
      int quantidade = salas.get(sala);
      
      salas.put(sala,quantidade++);
      userArray[id].setRoom(sala);
      printarErro("OK\n", id);
      mandarTodos(userArray[id].nome, "JOINED ", sala, id, 1);
    }
  }

  public static void printarErro(String message1, int id) throws IOException{
      buffer.clear();
      String message2= message1;
      buffer.put(message2.getBytes());
      buffer.flip();
      while(buffer.hasRemaining()){
        userArray[id].channel.write(buffer);
      }
      buffer.clear();
  }

  public static void mandarTodos(String message1, String choice, String sala, int id, int flag) throws IOException{

    String message2="null";
    if(flag==1){
      message2= choice+message1+"\n";
    }
    if(flag==0){
      message2= choice+" "+message1+"\n";  
    }
    buffer.clear();
    buffer.put(message2.getBytes());
    Set<SelectionKey> selectedKeys = selector.keys();
   //iterar nos canais
    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
    buffer.flip();
   
    while(keyIterator.hasNext()){
      SelectionKey key = keyIterator.next();
     
      if(key.isAcceptable())
        continue;

      SocketChannel sc = (SocketChannel)key.channel();
      Socket temp_s = sc.socket();
      int myport = temp_s.getPort();
      Object objeto = key.attachment();
      int teste = (int) objeto;
      if(userArray[id].port != myport && userArray[teste].sala.equals(sala)){
      sc.write(buffer);
      buffer.rewind();
      }
    }
    buffer.clear();
  }

  public static void leave(int id) throws IOException{
    if(userArray[id].estado==2){
		printarErro("OK\n", id);
      mandarTodos(userArray[id].nome, "LEFT ", userArray[id].sala, id, 1);
      userArray[id].leaveRoom();
    }
    else{
      printarErro("ERROR\n", id);
    }
  }

  public static void customLeave(int id) throws IOException{
    if(userArray[id].estado==2){
      printarErro("BYE\n", id);
      mandarTodos(userArray[id].nome, "LEFT ", userArray[id].sala, id, 1);
      userArray[id].leaveRoom();
      Socket s = userArray[id].channel.socket();
      System.out.println( "Closing connection to "+s );
      s.close();

    }
    else{
      Socket s = null;
      try{
        printarErro("BYE\n", id);
        s = userArray[id].channel.socket();
        System.out.println( "Closing connection to "+s );
        s.close();
      }
      catch( IOException ie ) {
        System.err.println( "Error closing socket " );
      }
    }
  }


}
