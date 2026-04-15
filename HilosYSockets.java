//Joaquín Devige.
//Legajo: 114638.

package hilosysockets;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HilosYSockets {

    public static void main(String[] args) throws Exception{

        Scanner teclado = new Scanner(System.in);          //Lee lo que el usuario escribe por consola.

        Socket sc = new Socket("127.0.0.1", 5500);         //Se conecta al servidor en localhost, puerto 5500.
        System.out.println("¡Conectado al servidor!\n");

        DataInputStream  entrada = new DataInputStream(sc.getInputStream());   //Para LEER mensajes del servidor.
        DataOutputStream salida  = new DataOutputStream(sc.getOutputStream()); //Para ENVIAR mensajes al servidor.

        while (true) {                                     //Bucle principal: repite hasta que la sesión termine.

            String respServidor = entrada.readUTF();       //Espera y lee el mensaje que manda el servidor.
            System.out.println("\n[SERVIDOR]\n" + respServidor); //Muestra el mensaje en la consola del cliente.

            if (respServidor.contains("¡Hasta luego!")) { //Si el servidor mandó la despedida, termina.
                break;                                     //Sale del bucle: fin del programa.
            }

            System.out.print("[YO] > ");                  //Muestra el prompt para que el usuario escriba.
            String respuesta = teclado.nextLine().trim();  //Lee la respuesta que escribe el usuario.

            salida.writeUTF(respuesta);                    //Envía la respuesta al servidor.
        }

        sc.close();                                        //Cierra el socket: libera la conexión.
        System.out.println("Conexión cerrada.");   //Mensaje final en la consola del cliente.
    }
}
