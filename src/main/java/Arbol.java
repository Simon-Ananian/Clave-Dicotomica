import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.View;
import org.json.JSONException;

//Clase que representa el arbol binario
class Arbol {
    
    //Clase interna que representa los nodos del Árbol
    class Nodo {
        String pregunta;
        Nodo verdadero;
        Nodo falso;
        String especie;
        
        public Nodo(String pregunta) {
            this.pregunta = pregunta;
            this.verdadero = null;
            this.falso = null;
            this.especie = null;
        }
    }

    private Nodo raiz; //Representa el nodo raiz del Árbol Binario
    private int contador = 0; // Para generar IDs únicos en el grafo
    private Graph graph;       // Referencia al grafo en el que se mostrarán los nodos
    private TablaHash tablaHash;      // Referencia a la tabla Hash
    private String[] preguntasRuta = new String[200]; //Arreglo para representar las preguntas
    boolean encontroEspecie = false;
    private int nivel = 0;
    
    public Arbol() {
        this.raiz = null;
    }
    
    public void setGraph(Graph g) {
        this.graph = g;
    }

    // Este metodo se encarga de leer el JSON y cargar la información en Árbol Binario y la Tabla Hash
    public boolean cargarDesdeArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
        int resultado = fileChooser.showOpenDialog(null); //Lee el archivo
        if (resultado == JFileChooser.APPROVE_OPTION) {
            String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                String contenido = new String(Files.readAllBytes(Paths.get(rutaArchivo)));
                // Validar si el contenido es un JSON válido
                try {
                    JSONObject json = new JSONObject(contenido); 
                    String nombreArreglo = json.keySet().iterator().next();
                    JSONArray especies = json.getJSONArray(nombreArreglo);
                    TablaHash tablaHash = new TablaHash(especies.length());
                    tablaHash.construirDesdeJSON(json, especies); //Carga el JSON en la Tabla Hash
                    this.tablaHash = tablaHash;
                    construirDesdeJSON(json, especies); // Carga el JSON en el Arbol
                    return true;
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(null, "El archivo no contiene un JSON válido.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error al leer el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return false;
            }
        } else {
            JOptionPane.showMessageDialog(null, "No se seleccionó ningún archivo", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    // Este metodo se encarga de cargar la información del JSON en el Arbol Binario
    public void construirDesdeJSON(JSONObject json, JSONArray especies) {
        for (int i = 0; i < especies.length(); i++) {
            JSONObject especieObj = especies.getJSONObject(i);
            String especie = especieObj.keys().next();
            JSONArray preguntas = especieObj.getJSONArray(especie);  //Por cada especie obtiene el arreglo de preguntas        
            if (this.raiz == null) {
                this.raiz = construirNodo(preguntas, especie, 0); //Crea el nodo correspondiente de la pregunta raiz
            } else {
                insertarEnArbol(raiz, preguntas, especie, 0); //Crea e inserta los siguientes nodos del arbol
            }
        }
    }

    // Este metodo se encarga de crear el nodo de la pregunta / especie del arbol
    private Nodo construirNodo(JSONArray preguntas, String especie, int indice) {
        if (indice >= preguntas.length()) {
            Nodo nodoHoja = new Nodo(null);
            nodoHoja.especie = especie;
            return nodoHoja;
        }
        JSONObject preguntaObj = preguntas.getJSONObject(indice);
        String pregunta = preguntaObj.keys().next();
        boolean respuesta = preguntaObj.getBoolean(pregunta);
        Nodo nodo = new Nodo(pregunta);
        if(respuesta){
           nodo.verdadero = construirNodo(preguntas, especie, indice + 1);
        } else{
           nodo.falso = construirNodo(preguntas, especie, indice + 1);
        }
        return nodo;
    }

    private void insertarEnArbol(Nodo actual, JSONArray preguntas, String especie, int indice) {
        if (indice >= preguntas.length()) {
            return;
        }
        JSONObject preguntaObj = preguntas.getJSONObject(indice);
        String pregunta = preguntaObj.keys().next();
        boolean respuesta = preguntaObj.getBoolean(pregunta);
        if (actual.pregunta != null) {
            if (actual.pregunta.equals(pregunta)) {
                if (!respuesta) {
                    if (actual.falso == null) { //Si el nodo que apunta a "actua.falso" no existe, entonces crea un nuevo nodo. (Esta validación, evita que se crean nodos que ya existen)
                        actual.falso = construirNodo(preguntas, especie, indice + 1);
                    } else { //Si ya existe, entonces se mueve a ese nodo para seguir construyendo el arbol
                        insertarEnArbol(actual.falso, preguntas, especie, indice + 1);
                    }
                } else {
                    if (actual.verdadero == null) { //Si el nodo que apunta a "actua.verdadero" no existe, entonces crea un nuevo nodo. (Esta validación, evita que se crean nodos que ya existen)
                        actual.verdadero = construirNodo(preguntas, especie, indice + 1);
                    } else { //Si ya existe, entonces se mueve a ese nodo para seguir construyendo el arbol
                        insertarEnArbol(actual.verdadero, preguntas, especie, indice + 1);
                    }
                }
            }
        }
    }
    
    // Busca una especie del arbol recursivamente (por preorden), imprime las preguntas y el tiempo que le tomo encontrarlo.
    private void buscarEspecie(Nodo R, String nombreEspecie, int nivel) {
        long inicio = System.nanoTime();
        if (R != null) { //Si el nodo no es null, entonces sigue buscando
            if (R.especie != null && R.especie.equals(nombreEspecie)) { //Si el nombre de la especie es la especie buscada, entonces imprime el mensaje
                encontroEspecie = true;
                StringBuilder mensaje = new StringBuilder("Especie encontrada: " + R.especie + "\nPreguntas que llevaron a esta especie:\n");
                for (int i = 0; i < nivel; i++) {
                    if (preguntasRuta[i] != null) {
                        mensaje.append(" - ").append(preguntasRuta[i]).append("\n");
                    }
                }
                long fin = System.nanoTime();
                mensaje.append("\nTiempo de búsqueda en Árbol: ").append((fin - inicio) / 1_000_000.0).append(" ms");
                JOptionPane.showMessageDialog(null, mensaje.toString(), "Búsqueda de Especie por recorrido del Árbol", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (R.pregunta != null) {        
                preguntasRuta[nivel] = "¿"+ R.pregunta + "?: " + "No"; //Antes de que recorra la parte izquierda del Arbol, guarda la pregunta del nodo actual en el arreglo con el valor por defecto en "No"      
                // Explorar el nodo "falso"
                buscarEspecie(R.falso, nombreEspecie, nivel + 1);    
                
                preguntasRuta[nivel] = "¿"+ R.pregunta + "?: " + "Sí"; //Al momento de volver en la función recursiva para recorrer la parte derecha del Arbol, como no ha encontrado la especie, va a sobreescribir la pregunta con el valor de "Sí"             
                // Explorar el nodo "verdadero"
                buscarEspecie(R.verdadero, nombreEspecie, nivel + 1);
            }
        }
    }

    
    //Calcula la profundidad del arbol para ajustar el tamaño de la interfaz
    private int calcularProfundidad(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int profundidadIzquierda = calcularProfundidad(nodo.falso);
        int profundidadDerecha = calcularProfundidad(nodo.verdadero);
        return Math.max(profundidadIzquierda, profundidadDerecha) + 1; // Devuelve el nivel más profundo
    }
    
    //Metodo que ajusta las dimensiones de la interfaz e invoca a la funcion preOrdenRecursivo
    public void preOrden() {
        int profundidad = calcularProfundidad(raiz);
        // Inicia el recorrido desde la raíz, con una posición central y una profundidad inicial
        int anchoFrame = Math.max(500, 350 * profundidad); // Define el ancho del frame
        int altoInicial = 400;  // La posición inicial en la parte superior del frame
        preOrdenRecursivo(raiz, null, anchoFrame / 2, altoInicial, anchoFrame / 4);
    }

    // Método para recorrer el árbol en preorden y asignar posiciones fijas
    private void preOrdenRecursivo(Nodo R, String idPadre, int x, int y, int offset) {
        if (R != null) {
            // El método visitar ahora agrega el nodo al grafo y devuelve su ID
            String idActual = visitar(R, idPadre, x, y);        
            if(offset<40){ // Ajusta el minimo valor de distancia entre nodos
                offset = 40;
            }
            // Recursión para nodos "falso" (hacia la izquierda)
            preOrdenRecursivo(R.falso, idActual, x - offset, y - 100, offset / 2);
            // Recursión para nodos "verdadero" (hacia la derecha)
            preOrdenRecursivo(R.verdadero, idActual, x + offset, y - 100, offset / 2);
                         
        }
    }

    // Establece la posición del nodo
    private String visitar(Nodo R, String idPadre, int x, int y) {
        String idActual = "n" + (contador++);
        String etiqueta = (R.especie != null) ? R.especie : R.pregunta;

        // Crear el nodo con su etiqueta y asignar posición fija
        graph.addNode(idActual).setAttribute("ui.label", etiqueta);
        graph.getNode(idActual).setAttribute("xyz", x, y, 0); // Asigna posición fija en el plano 2D
        
        if (idPadre != null) {
            // Crear la arista entre el nodo padre y el actual
            graph.addEdge(idPadre + "_" + idActual, idPadre, idActual, true);
        }

        return idActual;
    }

    // Método para convertir el árbol a un grafo sin layout automático
    public Graph toGraph() {
        Graph g = new SingleGraph("Arbol PreOrden");

        // Estilo para los nodos
        g.addAttribute("ui.stylesheet", "node { fill-color: green; text-size: 15; text-style: bold; text-alignment: left; } ");
        setGraph(g);
        contador = 0;

        // Recorrido en preorden para asignar posiciones fijas
        preOrden();

        g.addAttribute("layout.frozen", true); //Desactiva el ajuste automático
        
        // Calcula el tamaño del frame dinámicamente
        int profundidad = calcularProfundidad(raiz);
        int anchoFrame = Math.max(500, 400 * profundidad);

        JFrame frame = new JFrame("Visualización del Árbol");
        frame.setSize(anchoFrame, 600); // Tamaño estático definido (ancho x alto)
        
        // Obtener la vista sin crear una nueva ventana
        Viewer viewer = new Viewer(g, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.disableAutoLayout();
        View view = viewer.addDefaultView(false);
        
        // Crear botón "Buscar Especie"
        JButton btnBuscar = new JButton("Buscar Especie");
        btnBuscar.setFont(new Font("Arial", Font.BOLD, 12));
        btnBuscar.setBackground(Color.YELLOW); // Botón amarillo
        btnBuscar.setPreferredSize(new Dimension(200, 30)); // Tamaño más corto
        btnBuscar.setMargin(new Insets(2, 10, 2, 10));
        
        btnBuscar.addActionListener(new ActionListener() {
        @Override
            public void actionPerformed(ActionEvent e) {
                String[] opciones = {"Buscar por Hash", "Buscar por Árbol"};           
                int eleccion = JOptionPane.showOptionDialog(
                    frame,
                    "Seleccione el método de búsqueda:",
                    "Método de Búsqueda",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opciones,
                    null
                );
                 // Salir si el usuario cierra el cuadro de diálogo
                if (eleccion == -1) {
                    return;
                }               
                String especieBuscada;
                do {
                    especieBuscada = JOptionPane.showInputDialog(frame, "Ingrese el nombre de la especie:");
                    if (especieBuscada == null) {
                        return;
                    }
                    if (especieBuscada.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "El campo no puede estar vacío.\n Ingrese un nombre válido.", "Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }   
                    break; 
                } while (true);                   
                if (eleccion == 0) { // Buscar por Hash
                    tablaHash.buscarEspecie(especieBuscada);
                } else if (eleccion == 1){
                    preguntasRuta = new String[100];
                    encontroEspecie = false;
                    nivel=0;
                    buscarEspecie(raiz, especieBuscada, nivel);
                    if(!encontroEspecie){
                        JOptionPane.showMessageDialog(null, "Especie no encontrada o no hay suficientes preguntas para identificarla", 
                        "Búsqueda de Especie por recorrido del Árbol", JOptionPane.WARNING_MESSAGE);
                    }     
                }    
            }
        });
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSuperior.setBackground(Color.WHITE);
        panelSuperior.add(btnBuscar);
        frame.add(panelSuperior, BorderLayout.NORTH);
        frame.add((Component) view, BorderLayout.CENTER);
        frame.setVisible(true);
        return g;
    }
}