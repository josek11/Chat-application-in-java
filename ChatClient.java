import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.net.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );
	private final Charset charset = Charset.forName("UTF8");
    private final SocketChannel socketChannel; 
    private final CharsetDecoder decoder = charset.newDecoder();
    public static int check = 0; // 1- /nick || 2- /join
    public static int checknick = 0; //Ver se o ERRO de nick já foi printado
    public static int ready = 0; // 0- Sem nick ou sem sala ou sem nada feito || 1- Tudo pronto
    public static int nick_created = 0; // 0-Nick ainda não foi criado | 1-Nick já criado
    public static int left = 0; // Se saiu da sala
    public static String nickname;
    public static String nickname2; //Nick antigo
    public static String sala;

    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
    	String message2 = "";

    	//Verificar que tipo de mensagem é
    	if(message.equals("OK\n")) {
    		if(check == 1) { // Nick
    			if(ready == 1) {
	    			message2 = "Mudou de nome para " + nickname + "\n";
	    		}
    			else message2 = "Registado com o nome " + nickname + "\n";
    			nick_created = 1;
    		}
    		if(check == 2 && left == 0) { // Join
    			ready = 1;
    			left = 0;
    			message2 = "Entrou na sala " + sala + "\n";
    		}	
    		if(left == 1) {
    			message2 = "Saiu da sala " + sala + "\n";
    		}
    	}
    	else if(message.equals("ERROR\n")) {
    		if(check == 1) {
    			check = 0;
    			checknick = 1;
    			message2 = "Nome " +  nickname + " já utilizado" + "\n";
    		}
    		else if(ready == 0 && nick_created == 1) {
    			message2 = "Não está numa sala. Utilize o comando /join" + "\n";
    		}
    		else message2 = "Crie um nick através do comando /nick" + "\n";
    	}
    	else if(message.startsWith("MESSAGE")) {
    		String temp = message.replace("MESSAGE ", "");
    		int tam = temp.indexOf(" ");
    		message2 = temp.substring(0, tam) + ": " + temp.substring(tam+1);
    	}
    	else if(message.startsWith("NEWNICK")) {
    		String temp = message.replace("NEWNICK ", "");
    		int tam = temp.indexOf(" ");
    		String nickantigo = temp.substring(0, tam) + " ";
    		String temp2 = temp.replace(nickantigo , "");
    		message2 = temp.substring(0, tam) + " mudou de nome para " + temp2;
    	}
    	else if(message.startsWith("JOINED")) {
    		String temp = message.replace("JOINED ", "");
    		int tam = temp.indexOf("\n");
    		message2 = temp.substring(0,tam) + " entrou na sala" + "\n";
    	}
    	else if(message.startsWith("LEFT")) {
    		String temp = message.replace("LEFT ", "");
    		int tam = temp.indexOf("\n");
    		message2 = temp.substring(0,tam) + " saiu da sala" + "\n";
    	}

        chatArea.append(message2);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
		socketChannel = SocketChannel.open(new InetSocketAddress(server, port));
		socketChannel.configureBlocking(true);
    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
		//String mensagem = message+"\n";
	    if(message.startsWith("/nick")) {
	    	check = 1;
	    	nickname2 = nickname;
	    	nickname = message.substring(6);
	    }	
	    else if(message.startsWith("/join") && check == 1) { // Escolher sala sem estar nenhuma atribuida
	    	check = 2;
	    	sala = message.substring(6);
	    }
	    else if(message.startsWith("/join") && check == 2) { // Escolher uma sala estando já numa
	    	sala = message.substring(6);
	    }
	    else if(message.startsWith("/join") && nick_created == 1) {
	    	check = 2;
	    	sala = message.substring(6);
	    }
	    else if(message.startsWith("/leave")) {
	    	left = 1;
	    	ready = 0;
	    }

		buffer.put(message.getBytes());
		buffer.flip();
		while(buffer.hasRemaining()) {
			socketChannel.write(buffer);
		}
		buffer.clear();
    }

    //printmessage imprime a mensagem na janela
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
		chatArea.append("Connection estabilished\n");
		while(true){
			socketChannel.read(buffer);
			buffer.flip();
			String minhamensagem = decoder.decode(buffer).toString();
			if(minhamensagem.equals("BYE\n")) break;
			printMessage(minhamensagem);
			buffer.clear();	
		}
		frame.dispose();
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
