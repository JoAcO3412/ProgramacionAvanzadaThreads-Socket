//Joaquín Devige.
//Legajo: 114638.


package hilosysockets;

import java.io.*;      //Contiene las herramientas necesarias para que el programa pueda leer y enviar datos (Input y Output).
import java.net.*;     //Contiene todas las herramientas necesarias para que el programa se conecte a internet o en este caso a una red local mediante LocalHost 5500.

public class HilosYSocketsServer {

    // Banco de preguntas: (pregunta, opA, opB, opC, opD, respuestaCorrecta).
    public static final String[][] PREGUNTAS ={
        {"¿Cuánto es 5 + 3?",                "A)6",  "B)8",   "C)9",  "D)7",  "B"},
        {"¿Capital de Francia?",              "A)Roma","B)Madrid","C)París","D)Berlín","C"},
        {"¿Lados de un hexágono?",            "A)5",  "B)7",   "C)8",  "D)6",  "D"},
        {"¿Quién escribió el Quijote?",       "A)Lope","B)Cervantes","C)Quevedo","D)Góngora","B"},
        {"¿Planeta más grande?",              "A)Saturno","B)Neptuno","C)Júpiter","D)Urano","C"},
        {"¿Cuánto es 12 x 12?",              "A)144","B)124",  "C)132","D)148","A"},
        {"¿Año llegada del hombre a la Luna?","A)1965","B)1972","C)1969","D)1971","C"},
        {"¿Océano más grande?",              "A)Atlántico","B)Índico","C)Ártico","D)Pacífico","D"},
        {"¿Colores del arcoíris?",            "A)5",  "B)6",   "C)7",  "D)8",  "C"},
        {"¿Símbolo químico del oro?",         "A)Go", "B)Or",  "C)Ag", "D)Au", "D"}
    };

    public static void main(String[] args) throws Exception{

        ServerSocket servidor = new ServerSocket(5500);                 //Abre el servidor escuchando en el puerto 5500.
        System.out.println("Servidor iniciado en puerto 5500...");

        while(true){                                                                       //Bucle infinito: siempre espera nuevos clientes.
            Socket cliente = servidor.accept();                                           //Se bloquea aquí hasta que alguien se conecte.
            System.out.println("Cliente conectado: " + cliente.getInetAddress());         //Muestra la IP del cliente.
            atenderCliente(cliente);                                                      //Llama al método que maneja toda la sesión.
            System.out.println("Cliente desconectado.\n");                                //Avisa cuando el cliente se fue.
        }
    }

   public static void atenderCliente(Socket cliente) throws Exception{

        //Streams para comunicarse con el cliente
        DataInputStream  entrada = new DataInputStream(cliente.getInputStream());        //Para LEER lo que manda el cliente.
        DataOutputStream salida  = new DataOutputStream(cliente.getOutputStream());      //Para ESCRIBIR al cliente.

        int puntaje = 0;   //Guarda cuántas respuestas correctas lleva el cliente.
        int idx = 0;       //Índice de la pregunta actual (empieza en 0).

        //Envía bienvenida con la primera pregunta ya adjunta.
        salida.writeUTF("¡BIENVENIDO AL PREGUNTADOS!\n" + "Responde con A, B, C o D.\n" + "SALIR para terminar\n\n" + formatear(idx, puntaje));

        while(true){   //Bucle principal: espera y procesa cada respuesta.

            String msg = entrada.readUTF().trim().toUpperCase();     //Lee el mensaje del cliente y lo pasa a mayúsculas.
            System.out.println("[CLIENTE] " + msg);                  //Log en la consola del servidor.

            //Condicional para SALIR.
            
            if(msg.equals("SALIR")){     //El cliente quiere terminar la sesión.
                salida.writeUTF("Puntaje final: " + puntaje + "/" + PREGUNTAS.length + " ¡Hasta luego!");
                break;                                           //Sale del bucle: se cierra la sesión.
            }

            //Condicional para la RESPUESTA A / B / C / D.
            
            if(msg.matches("[ABCD]")){                         //Verifica que la respuesta sea una de las 4 letras válidas.
                boolean correcto = msg.equals(PREGUNTAS[idx][5]);//Compara con la respuesta correcta del array.
                String feedback;

                if(correcto){       //Si acertó.
                    puntaje++;      //Suma 1 punto.
                    feedback = "¡CORRECTO! Puntaje: " + puntaje + "/" + PREGUNTAS.length + "\n\n";
                } else{      //Si falló.
                    feedback = "INCORRECTO. Era: " + PREGUNTAS[idx][5] + " | Puntaje: " + puntaje + "/" + PREGUNTAS.length + "\n\n";
                }

                idx++;   //Pasa a la siguiente pregunta.

                if(idx < PREGUNTAS.length){    //Si aún quedan preguntas por responder.
                    //Envía el resultado y la siguiente pregunta en un solo mensaje para no desincronizar al cliente.
                    salida.writeUTF(feedback + formatear(idx, puntaje)); 
                } else{    //Si ya respondió todas las preguntas.
                    salida.writeUTF(feedback + "=== FIN DEL JUEGO ===\nPuntaje final: " + puntaje + "/" + PREGUNTAS.length + "\nEscribe SALIR para terminar.");
                }

            } else{   //El mensaje no fue A, B, C, D ni un comando.
                salida.writeUTF("Responde solo A, B, C o D  (o SALIR)\n\n" + formatear(idx, puntaje));
            }
        }

        cliente.close();     //Cierra el socket: termina la conexión con ese cliente.
    }

    //Arma el texto de la pregunta con sus 4 opciones para enviar al cliente.
    public static String formatear(int idx, int puntaje){
        
        String[] p = PREGUNTAS[idx];                                 //Toma la fila completa de la pregunta actual.
        return "── Pregunta " + (idx + 1) + "/" + PREGUNTAS.length
             + "  |  Puntaje: " + puntaje + " ──\n"
             + p[0] + "\n"                                           //Texto de la pregunta.
             + p[1] + "   " + p[2] + "   " + p[3] + "   " + p[4]     //Las 4 opciones en una sola línea.
             + "\nTu respuesta: ";
        
    }
}
