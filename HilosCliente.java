//Joaquín Devige.
//Legajo: 114638.

package hilosysockets;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HilosYSockets {

    public static void main(String[] args) throws Exception {

        Scanner teclado = new Scanner(System.in);
        Socket sc = new Socket("127.0.0.1", 5500);
        System.out.println("¡Conectado al servidor!\n");
        
        //Estas clases nos permiten usar métodos como readUTF() y writeUTF()
        //para enviar y recibir texto de forma directa.

        DataInputStream  entrada = new DataInputStream(sc.getInputStream());
        DataOutputStream salida  = new DataOutputStream(sc.getOutputStream());

        //Hilo lector.
        //Se ejecuta en paralelo al hilo principal.
        //Su única tarea: esperar mensajes del servidor e imprimirlos.
        Thread lector = new Thread(() -> {
            try {
                while(true){
                    //Se queda esperando mensajes del servidor y los imprime en pantalla.
                    //El metodo readUTF() detiene este hilo especifico hasta que llega un mensaje, 
                    //lo imprime y vuelve a esperar.
                    String msg = entrada.readUTF();
                    
                    System.out.println("\n[SERVIDOR] " + msg);
                }
            } catch (Exception e) {
                System.out.println("\nConexión cerrada por el servidor.");
            }
        });
        
        //Un hilo Daemon (demonio) es un hilo "secundario". 
        //Si el hilo principal termina, el setDaemon() mata a todos los hilos Daemon automáticamente y cierra el programa.
        lector.setDaemon(true); //Se cierra automáticamente al terminar el programa.
        lector.start();

        //Hilo principal (escritura).
        //Lee lo que escribe el usuario y lo envía al servidor.
        while(true){
       
            String respuesta = teclado.nextLine().trim();
            
            //Si el usuario apretó enter sin escribir nada, 
            //salta a la siguiente iteración del bucle.
            if(respuesta.isEmpty()){
                
              continue;  //Ignorar líneas vacías.
            } 
            

            salida.writeUTF(respuesta);  //Le manda la respuesta del usuario al servidor.

            if(respuesta.equalsIgnoreCase("SALIR")){
                
              break;  
            } 
            
        }

        sc.close();
        System.out.println("Conexión cerrada.");
    }
}
