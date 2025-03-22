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
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.View;


class Arbol {
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

    private Nodo raiz;
    private int contador = 0; // Para generar IDs únicos en el grafo
    private Graph graph;       // Referencia al grafo en el que se mostrarán los nodos
    private String[] preguntasRuta = new String[100];
    private int nivel = 0;
    
    public Arbol() {
        this.raiz = null;
    }
    
    public void setGraph(Graph g) {
        this.graph = g;
    }

    public void cargarDesdeArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        int resultado = fileChooser.showOpenDialog(null);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                String contenido = new String(Files.readAllBytes(Paths.get(rutaArchivo)));
                construirDesdeJSON(contenido);
                System.out.println(contenido);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void construirDesdeJSON(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String nombreArreglo = json.keySet().iterator().next();
        JSONArray arboles = json.getJSONArray(nombreArreglo);
        
        for (int i = 0; i < arboles.length(); i++) {
            JSONObject especieObj = arboles.getJSONObject(i);
            String especie = especieObj.keys().next();
            JSONArray preguntas = especieObj.getJSONArray(especie);
            
            if (this.raiz == null) {
                this.raiz = construirNodo(preguntas, especie, 0);
            } else {
                insertarEnArbol(raiz, preguntas, especie, 0);
            }
        }
    }

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
                    if (actual.falso == null) {
                        actual.falso = construirNodo(preguntas, especie, indice + 1);
                    } else {
                        insertarEnArbol(actual.falso, preguntas, especie, indice + 1);
                    }
                } else {
                    if (actual.verdadero == null) {
                        actual.verdadero = construirNodo(preguntas, especie, indice + 1);
                    } else {
                        insertarEnArbol(actual.verdadero, preguntas, especie, indice + 1);
                    }
                }
            }
        }
    }
        
    private void buscarEspecie(Nodo R, String nombreEspecie, boolean lado) {
        if (R != null) {
            if(R.especie != null && R.especie.equals(nombreEspecie)){
                StringBuilder mensaje = new StringBuilder("Especie encontrada: " + R.especie + "\nPreguntas que llevaron a esta especie:\n");
                for (int i = 0; i < preguntasRuta.length; i++) {
                    if(preguntasRuta[i] != null){
                       mensaje.append(" - ").append(preguntasRuta[i]).append("\n");
                    }
                }
                JOptionPane.showMessageDialog(null, mensaje.toString(), "Búsqueda de Especie", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if(R.pregunta != null){
                boolean continuarBusqueda = true;
                if (R.falso == null && R.verdadero == null) {
                    continuarBusqueda = false;
                }
                if (R.falso == null){
                    if(R.verdadero.especie != null) {
                        if (!R.verdadero.especie.equals(nombreEspecie)){
                            continuarBusqueda = false;
                        }
                    }
                } else if (R.verdadero == null) {
                    if(R.falso.especie != null) {
                        if (!R.falso.especie.equals(nombreEspecie)){
                            continuarBusqueda = false;
                        }
                    }
                }
                
                if(R.falso != null && R.verdadero != null) {
                    if (R.falso.especie != null && R.verdadero.especie != null) {
                        if(!R.falso.especie.equals(nombreEspecie) && !R.verdadero.especie.equals(nombreEspecie)) {
                            continuarBusqueda = false;
                        }
                    }
                }
                
                if(continuarBusqueda){
                    preguntasRuta[nivel] = R.pregunta; 
                    nivel++;
                } else if (lado) { 
                    preguntasRuta[nivel-1] = null;
                    nivel--;
                }
            }    
            
            buscarEspecie(R.falso, nombreEspecie, false);
            buscarEspecie(R.verdadero, nombreEspecie, true);
        }
    }

    
    private int calcularProfundidad(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int profundidadIzquierda = calcularProfundidad(nodo.falso);
        int profundidadDerecha = calcularProfundidad(nodo.verdadero);
        return Math.max(profundidadIzquierda, profundidadDerecha) + 1; // Devuelve el nivel más profundo
    }
    
    
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
                      
            if(offset<40){
                offset = 40;
            }
            // Recursión para nodos "falso" (hacia la izquierda)
            preOrdenRecursivo(R.falso, idActual, x - offset, y - 100, offset / 2);
            // Recursión para nodos "verdadero" (hacia la derecha)
            preOrdenRecursivo(R.verdadero, idActual, x + offset, y - 100, offset / 2);
                         
        }
    }

    // Ajusta el método visitar para establecer posiciones fijas en el grafo
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

        g.addAttribute("layout.frozen", true);
        
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
                String especieBuscada = JOptionPane.showInputDialog(frame, "Ingrese el nombre de la especie:");
                preguntasRuta = new String[100];
                nivel=0;
                if (especieBuscada != null && !especieBuscada.trim().isEmpty()) {
                    buscarEspecie(raiz, especieBuscada, false);
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