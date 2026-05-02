//Nombre: Joaquín Devige.
//Legajo: 114638.

package hilosysockets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClienteGUI extends JFrame implements Runnable{
    
    private JTextField txtUsuario;
    private JTextField txtHost;
    private JTextField txtPuerto;
    private JTextArea areaChat;
    private JTextField txtMensaje;
    private JButton btnConectar;
    private JButton btnEnviar;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String nombreUsuario;

    public ClienteGUI(){
        super("Chat Cliente - Sockets e Hilos");
        configurarInterfaz();
    }

    private void configurarInterfaz(){
        setSize(450, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        //Panel superior: Conexión y Validaciones.
        JPanel panelConexion = new JPanel(new GridLayout(3, 2, 5, 5));
        panelConexion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panelConexion.add(new JLabel("Usuario:"));
        txtUsuario = new JTextField();
        panelConexion.add(txtUsuario);

        panelConexion.add(new JLabel("Host:"));
        txtHost = new JTextField("127.0.0.1");
        panelConexion.add(txtHost);

        panelConexion.add(new JLabel("Puerto:"));
        txtPuerto = new JTextField("5500");
        panelConexion.add(txtPuerto);

        btnConectar = new JButton("Conectar");
        
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelConexion, BorderLayout.CENTER);
        panelSuperior.add(btnConectar, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        //Panel central: Área de Chat.
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        add(new JScrollPane(areaChat), BorderLayout.CENTER);

        //Panel inferior: Envío de mensajes.
        JPanel panelEnvio = new JPanel(new BorderLayout(5, 5));
        panelEnvio.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtMensaje = new JTextField();
        txtMensaje.setEnabled(false);
        btnEnviar = new JButton("Enviar");
        btnEnviar.setEnabled(false);
        
        panelEnvio.add(txtMensaje, BorderLayout.CENTER);
        panelEnvio.add(btnEnviar, BorderLayout.EAST);
        add(panelEnvio, BorderLayout.SOUTH);

        //EVENTOS.
        btnConectar.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                conectarAlServidor();
            }
        });

        btnEnviar.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                enviarMensaje();
            }
        });
        
        //Permite enviar mensaje al presionar Enter.
        txtMensaje.addActionListener(e -> enviarMensaje());
    }

    //LÓGICA DE VALIDACIÓN.
    private boolean validarUsuario(String usuario){
        //Tamaño mínimo y máximo.
        if(usuario.length() < 3 || usuario.length() > 15){
            JOptionPane.showMessageDialog(this, "El usuario debe tener entre 3 y 15 caracteres.", "Error de Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        //Sin espacios en blanco.
        if(usuario.contains(" ")){
            JOptionPane.showMessageDialog(this, "El usuario no puede contener espacios en blanco.", "Error de Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        //Caracteres permitidos (solo letras y números) usando Expresión Regular.
        if (!usuario.matches("^[a-zA-Z0-9]+$")) {
            JOptionPane.showMessageDialog(this, "El usuario solo puede contener letras y números (sin caracteres especiales).", "Error de Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void conectarAlServidor(){
        String usuarioTemp = txtUsuario.getText();
        
        if(!validarUsuario(usuarioTemp)){
            return; //Detiene la conexión si la validación falla.
        }

        try{
            String host = txtHost.getText();
            int puerto = Integer.parseInt(txtPuerto.getText());
            
            socket = new Socket(host, puerto);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            nombreUsuario = usuarioTemp;
            
            //Envía el nombre de usuario al servidor al conectar.
            out.writeUTF(nombreUsuario);
            
            
            //Inicia el hilo para escuchar mensajes.
            Thread hiloEscucha = new Thread(this);
            hiloEscucha.start();

            //Actualiza la interfaz.
            btnConectar.setEnabled(false);
            txtUsuario.setEditable(false);
            txtHost.setEditable(false);
            txtPuerto.setEditable(false);
            txtMensaje.setEnabled(true);
            btnEnviar.setEnabled(true);
            txtMensaje.requestFocus();
            
           areaChat.append(nombreUsuario + " se ha unido al chat.\n");

        } catch(Exception ex){
            JOptionPane.showMessageDialog(this, "Error al conectar con el servidor: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarMensaje(){
        String mensaje = txtMensaje.getText().trim();
        if(!mensaje.isEmpty() && out != null){
            try {
                out.writeUTF(mensaje); 
                txtMensaje.setText("");
            } catch (IOException ex) {
                areaChat.append("Error al enviar el mensaje.\n");
            }
        }
    }

    //HILO PARA RECIBIR MENSAJES.
    @Override
    public void run(){
        try{
            while (true){
                String mensajeRecibido = in.readUTF();
                areaChat.append(mensajeRecibido + "\n");
                //Mueve el scroll al final.
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
            }
        } catch(IOException ex){
            areaChat.append("Desconectado del servidor.\n");
            btnConectar.setEnabled(true);
            txtMensaje.setEnabled(false);
            btnEnviar.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        //Ejecuta la Interfaz Grafica en el hilo de despacho de eventos de Swing.
        SwingUtilities.invokeLater(() -> {
            new ClienteGUI().setVisible(true);
        });
    }
}
