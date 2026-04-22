//Joaquín Devige.
//Legajo: 114638.


package hilosysockets;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class HilosYSocketsServer {

    //Mapa compartido: nombre: salida de cada cliente.
    public static ConcurrentHashMap<String, DataOutputStream> clientes = new ConcurrentHashMap<>();

    //Banco de preguntas.
    public static final String[][] PREGUNTAS = {
        {"¿Cuánto es 5 + 3?",                 "A)6",  "B)8",   "C)9",  "D)7",  "B"},
        {"¿Capital de Francia?",               "A)Roma","B)Madrid","C)París","D)Berlín","C"},
        {"¿Lados de un hexágono?",             "A)5",  "B)7",   "C)8",  "D)6",  "D"},
        {"¿Quién escribió el Quijote?",        "A)Lope","B)Cervantes","C)Quevedo","D)Góngora","B"},
        {"¿Planeta más grande?",               "A)Saturno","B)Neptuno","C)Júpiter","D)Urano","C"},
        {"¿Cuánto es 12 x 12?",               "A)144","B)124",  "C)132","D)148","A"},
        {"¿Año llegada del hombre a la Luna?", "A)1965","B)1972","C)1969","D)1971","C"},
        {"¿Océano más grande?",               "A)Atlántico","B)Índico","C)Ártico","D)Pacífico","D"},
        {"¿Colores del arcoíris?",             "A)5",  "B)6",   "C)7",  "D)8",  "C"},
        {"¿Símbolo químico del oro?",          "A)Go", "B)Or",  "C)Ag", "D)Au", "D"}
    };

    
    public static void main(String[] args) throws Exception {

        ServerSocket servidor = new ServerSocket(5500);
        System.out.println("Servidor iniciado en puerto 5500...");

        while (true){
            
            Socket cliente = servidor.accept();
            System.out.println("Nueva conexión desde:" + cliente.getInetAddress());

            //Un hilo nuevo por cada cliente que se conecta.
            Thread hilo = new Thread(() -> {
                try {
                    atenderCliente(cliente);
                } catch (Exception e) {
                    System.out.println("Error inesperado: " + e.getMessage());
                }
            });
            
            hilo.start();
        }
    }

    
    public static void atenderCliente(Socket cliente) throws Exception {

        DataInputStream  entrada = new DataInputStream(cliente.getInputStream());
        DataOutputStream salida  = new DataOutputStream(cliente.getOutputStream());

        //Pedir nombre.
        salida.writeUTF("Ingresá tu nombre de usuario: ");
        String deseado = entrada.readUTF().trim();
        String nombre  = asignarNombre(deseado);      //Nombre único garantizado.

        //Registrar al cliente en el mapa compartido.
        clientes.put(nombre, salida);

        //Avisar si el nombre tuvo que cambiar.
        String avisoNombre; //Creamos la variable de aviso.

        //Lo que se pregunta el sistema es: ¿El nombre final es exactamente igual al que el usuario deseaba?.
        if(nombre.equals(deseado)){
            //Si son iguales (verdadero), no hay que avisar nada.
            avisoNombre = "";

        } else{
            //Si son distintos (falso), armamos el mensaje de advertencia.
            avisoNombre = "\n  '" + deseado + "' ya existía. Tu nombre asignado es: " + nombre;
        }

        //Menú de bienvenida.
        salida.writeUTF(
            "\n════════════════════════════════════\n"
          + "   ¡BIENVENIDO AL PREGUNTADOS!\n"
          + "════════════════════════════════════\n"
          + "  Tu usuario: " + nombre + avisoNombre + "\n\n"
          + menu()
        );

        System.out.println("Registrado: " + nombre);
        broadcast("-> " + nombre + " se conectó.", nombre);  //Avisa a los demás.

        //Variables del juego (cada hilo tiene las suyas, no se mezclan).
        int puntaje = 0;
        int idx     = 0;
        boolean juegoActivo = false;   //El juego empieza solo si el cliente escribe jugar.

        //Bucle principal.
        while (true){

            String msg = entrada.readUTF().trim();  //Aca es donde el servidor atrapa la respuesta que el usuario envió.
            System.out.println("[" + nombre + "] " + msg);          //Print en consola del servidor.

            //SALIR.
            
            if(msg.equalsIgnoreCase("SALIR")){
                salida.writeUTF("¡Hasta luego, " + nombre + "!");
                break;
            }

            //AYUDA.
            
            if(msg.equalsIgnoreCase("AYUDA")){
                
                salida.writeUTF(menu());
                continue;
            }

            //FECHA.
            
            if(msg.equalsIgnoreCase("FECHA")){
                
                String fecha = new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new Date());
                salida.writeUTF("Fecha y hora: " + fecha);
                continue;
            }

            //LISTA.
            
            if(msg.equalsIgnoreCase("LISTA")){
                
                salida.writeUTF("Clientes conectados:\n" + listarClientes());
                continue;
            }

            //CALC <expresión>.
            
            if(msg.toUpperCase().startsWith("CALCULAR ")){
                
                String expr = msg.substring(5).trim();
                salida.writeUTF(calcular(expr));
                continue;
            }

            //*ALL <mensaje>: broadcast a todos.
            
            if(msg.toUpperCase().startsWith("*ALL ")){
                
                String texto = msg.substring(5).trim();
                
                if(texto.isEmpty()){
                    
                    salida.writeUTF(" El mensaje está vacío. Uso: *ALL hola a todos");
                } else{
                    
                    broadcast("[" + nombre + " -> TODOS]: " + texto, nombre);
                    salida.writeUTF(" Mensaje enviado a todos.");
                }
                continue;
            }
            
           

            //*usuario <mensaje>: mensaje privado.
            
            if(msg.startsWith("*")){
                
                //Formato esperado: *Juan hola como estás.
                String[] partes  = msg.substring(1).split(" ", 2);  //"Juan hola como estas", Saca el asterisco del principio.
                String destino = partes[0];  //Juan.
                String texto; 
                
                //Si el mensaje que escribe el usuario es mayor a un, es decir
                //que hay dos partes lo que seria correcto lo toma, sino manda una cadena vacia para avisar del error.
                if(partes.length > 1){
                    
                    texto = partes[1].trim();
                } else{
                    
                    texto = "";
                }
                 
                //Si el texto queda vacio se avisa el formato correcto; Ejemplo: *Juan.
                if(texto.isEmpty()){
                    
                    salida.writeUTF(" Formato: *usuario mensaje");
                    
                //Se valida que no se pueda escribir a uno mismo. Compara el destinatario con el nombre actual.
                //El ignoreCase evita que *juan y *Juan pasen como destinatarios distintos.
                } else if(destino.equalsIgnoreCase(nombre)){
                    
                    salida.writeUTF(" No podés mandarte mensajes a vos mismo.");
                } else{
                    //Busca al destinatario en el servidor con el concurrentHashMap.
                    //Si no lo encuentra devuelve null.
                    DataOutputStream salidaDestino = clientes.get(destino);

                    if(salidaDestino != null){
                        // El destinatario existe: enviar el mensaje normalmente.
                        salidaDestino.writeUTF("[" + nombre + "]: " + texto);
                        salida.writeUTF(" Mensaje enviado a " + destino + ".");
                    } else{
                        
                        //El destinatario NO existe: se busca una alternativa.
                        //Busca al primer usuario conectado que no sea el emisor.
                        //keySet: Extrae solo las claves del mapa (HashMap), es decir, solo los nombre.
                        //Devuelve un conjunto.
                        //stream: Convierte ese conjunto en una fila de elementos para procesar uno por uno.
                        //Se usa un filtro: recorre cada elemento del stream y descarta si no cumplen la condicion.
                        //n: Cada nombre (Juan, Ana, Pedro).
                        //!n.equals(nombre): Pregunta si este nombre actual si es el emisor, sino lo descarta y continua.
                        //Devuelve el primero que encuentra.
                        //orElse: si tenia un valar lo devuelve, si estaba vacio devuelve null.   
                        String alternativa = clientes.keySet().stream().filter(n -> !n.equals(nombre)).findFirst().orElse(null);
            
                        
                        if(alternativa != null){
                            
                            clientes.get(alternativa).writeUTF( "[" + nombre + " (redirigido de '" + destino + "'" + ")]: " + texto);
                            salida.writeUTF(" '" + destino + "' no existe. Mensaje redirigido a '" + alternativa + "'.");
                        } else{
                            
                            salida.writeUTF(destino + "' no existe y no hay otros usuarios conectados.");
                        }
                    }
                }
                continue;
            }

            //JUGAR: inicia el Preguntados.
            
            if(msg.equalsIgnoreCase("JUGAR")){
                
                juegoActivo = true;
                puntaje = 0;
                idx     = 0;
                salida.writeUTF("¡BIENVENIDO AL PREGUNTADOS!\nResponde con A, B, C o D.\n\n" + formatear(idx, puntaje));
                continue;
            }
            

            //Respuesta del juego (A / B / C / D).
            
            if(juegoActivo && msg.toUpperCase().matches("[ABCD]")){
                
                boolean correcto = msg.toUpperCase().equals(PREGUNTAS[idx][5]);
                String feedback;

                if(correcto){
                    
                    puntaje++;
                    feedback = "¡CORRECTO! Puntaje: " + puntaje + "/" + PREGUNTAS.length + "\n\n";
                } else{
                    
                    feedback = "INCORRECTO. Era: " + PREGUNTAS[idx][5] + " | Puntaje: " + puntaje + "/" + PREGUNTAS.length + "\n\n";
                }

                idx++;

                if(idx < PREGUNTAS.length){
                    
                    salida.writeUTF(feedback + formatear(idx, puntaje));
                } else{
                    
                    salida.writeUTF(feedback + "=== FIN DEL JUEGO ===\nPuntaje final: " + puntaje + "/" + PREGUNTAS.length + "\nEscribí AYUDA para ver comandos.");
                    juegoActivo = false;
                }
                continue;
            }

            //Cualquier otra cosa.
            salida.writeUTF(" Comando no reconocido. Escribí AYUDA para ver los comandos.");
        }

        //Limpieza al desconectar.
        clientes.remove(nombre);
        cliente.close();             //Cierra las conexiones entre el servidor y ese cliente en particular.
        System.out.println("Desconectado: " + nombre);
        broadcast("-> " + nombre + " se desconectó.", nombre);  //Manda este mensaje a todos los usuarios conectados.
        
    } //Fin bucle while.
    
    
    //Funcionamiento: Le asigna un sufijo a los nombres ya existentes y repetidos.
   public static synchronized String asignarNombre(String deseado){
       
    String nombreBase;

    //Se valida si el texto ingresado está vacío.
    if (deseado.isEmpty()){
        
        nombreBase = "Usuario"; //Le damos un nombre por defecto.
    } else{
        
        nombreBase = deseado;   //Usamos el nombre que eligió.
    }

    //Se verifica si ese nombre está libre en el servidor.
    boolean elNombreEstaOcupado = clientes.containsKey(nombreBase);
    
    if (elNombreEstaOcupado == false){
        
        //Si no está ocupado, devolvemos el nombre tal cual está y terminamos aquí.
        return nombreBase;
    }

    //Si se llegó a este punto, significa que el nombre ya existe.
    //Empezamos a buscar una alternativa agregando un número al final.
    int numeroSufijo = 2;
    String nombreAlternativo = nombreBase + " " + numeroSufijo; // Ejemplo: "Juan2"

    //Bucle: Mientras el nombre alternativo siga existiendo en la lista.
    while(clientes.containsKey(nombreAlternativo)){
        
        // Incrementamos el número (pasa de 2 a 3, luego a 4, etc.)
        numeroSufijo = numeroSufijo + 1; 
        
        // Volvemos a armar el nombre para que el bucle lo vuelva a evaluar
        nombreAlternativo = nombreBase + numeroSufijo; 
    }

    //Cuando el bucle termina, es porque se encontro un nombre libre.
    return nombreAlternativo;
}

    //Manda un mensaje a todos excepto al emisor.
    public static void broadcast(String msg, String emisor){
        
    //Se recorre el mapa de clientes uno por uno.
    //Map.Entry representa un "par" de datos dentro de nuestro mapa (NombreUsuario: FlujoDeSalida).
    for(Map.Entry<String, DataOutputStream> parCliente : clientes.entrySet()){
        
        //Extraemos los datos del cliente actual del bucle en variables separadas
        String nombreDestinatario = parCliente.getKey();             //La llave (el nombre).
        DataOutputStream salidaDestinatario = parCliente.getValue(); //El valor (para enviar los datos).
        
        //Comparamos el nombre del cliente actual con el nombre del emisor.
        boolean esElMismoEmisor = nombreDestinatario.equals(emisor);
        
        //Si no es la persona que mandó el mensaje original, se lo enviamos.
        if(esElMismoEmisor == false){
            
            //Intentamos enviar el mensaje. 
            //Usamos try-catch porque las conexiones de red son inestables.
            try {
                salidaDestinatario.writeUTF(msg);
                
            } catch (Exception errorConexion) {
                //Si falla (ej. al destinatario se le cortó el internet justo en ese milisegundo),
                //el catch está vacío de forma intencional. 
                //Esto permite ignorar el error de este usuario en particular y 
                //que el bucle 'for' continúe enviando el mensaje al resto de la lista sin que el servidor se caiga.
            }
        }
    }
  }

    //Lista los clientes conectados.
    public static String listarClientes(){
        
    //Primero verificamos si el mapa está vacío (si no hay nadie conectado).
    boolean noHayNadie = clientes.isEmpty();
    
    if(noHayNadie == true){
    //Si es verdad, devolvemos un texto avisando y terminamos el método aquí.
        return " (ninguno conectado)";
    }

    //Si llegamos acá, significa que SÍ hay clientes.
    //Usamos StringBuilder porque es la forma más eficiente y rápida de 
    //unir textos en Java cuando estamos dentro de un bucle.
    StringBuilder listaArmada = new StringBuilder();

    //Extraemos SOLO los nombres del mapa.
    //keySet() nos da un conjunto (Set) con todas las llaves (los nombres de usuario).
    Set<String> nombresConectados = clientes.keySet();

    //Recorremos ese conjunto de nombres uno por uno.
    for(String nombreDelCliente : nombresConectados){
        
        //Vamos agregando pedazos de texto al StringBuilder:
        listaArmada.append(" -- ");           
        listaArmada.append(nombreDelCliente); //Agregamos el nombre del cliente.
        listaArmada.append("\n");             //Agregamos un salto de línea para el siguiente.
    }

    //Se convierte el objeto StringBuilder en un texto normal (String).
    String textoFinal = listaArmada.toString();

    //El método trim() borra los espacios o saltos de línea que sobran al principio o al final.
    //(Esto evita que quede un salto de línea vacío al final de la lista).
    return textoFinal.trim();
  }
    

    //Resuelve una expresión matemática simple.
    public static String calcular(String expr){
        
        try {
            //Evalúa expresiones básicas: +, -, *, /, paréntesis.
            javax.script.ScriptEngine js = new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            Object resultado = js.eval(expr);
            return " " + expr + " = " + resultado;
        } catch (Exception e) {
            return "Expresión inválida: " + expr;
        }
    }

    //Log con hora en consola del servidor.
    public static void log(String msg){
        
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + hora + "] " + msg);
    }

    //Menú de ayuda.
    public static String menu(){
        
        return " COMANDOS:\n" 
             + "  JUGAR              -> Iniciar el juego Preguntados\n"
             + "  FECHA              -> Ver fecha y hora actual\n"
             + "  LISTA              -> Ver clientes conectados\n"
             + "  CALCULAR           -> Resolver expresión matemática\n"
             + "  *NombreUsuario msg -> Mensaje privado\n"
             + "  *ALL msg           -> Mensaje a todos\n"
             + "  AYUDA              -> Ver este menú\n"
             + "  SALIR              -> Desconectarse\n"
             + "════════════════════════════════════";
    }

    //Formatea la pregunta del juego.
    public static String formatear(int idx, int puntaje){
        
        String[] p = PREGUNTAS[idx];
        return " Pregunta " + (idx + 1) + "/" + PREGUNTAS.length + "  |  Puntaje: " + puntaje + " ──\n" + p[0] + "\n" + p[1] + "   " + p[2] + "   " + p[3] + "   " + p[4] + 
                "\nTu respuesta: ";
    }
}
