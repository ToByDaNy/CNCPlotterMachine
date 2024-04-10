package org.example;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Math.*;


public class InterfataProiect extends JFrame {

    public int x1 = 0;
    public int y1 = 0;
    public int CenterX = 0;
    public int CenterY = 0;
    double angle = 0;
    private ArrayList<Point> puncte;
    private ArrayList<Point> puncteLinii;
    private ArrayList<Point> puncteCurba;
    private boolean deseneazaLinii = true;
    private boolean deseneazaCurba = false;

    SerialPort sp ;

    public InterfataProiect(SerialPort ss) {
        sp = ss;
        puncte = new ArrayList<>();
        puncteLinii = new ArrayList<>();
        puncteCurba = new ArrayList<>();

        setTitle("Desenare Puncte cu Salvare și Linii");
        setSize(750, 750);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        sp.setComPortParameters(9600, 8, 1,0);
        //sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0,0);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                adaugaPunct(e.getPoint());
                repaint();
            }
        });

        JButton btnDeseneazaLinii = new JButton("Desenează Linii");
        btnDeseneazaLinii.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deseneazaLinii = true;
                deseneazaCurba = false;
            }
        });

        JButton btnDeseneazaCurba = new JButton("Desenează Curba");
        btnDeseneazaCurba.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deseneazaCurba = true;
                deseneazaLinii = false;
            }
        });
        JButton btnTrimitereFisier = new JButton("Scriere in fisier");
        btnTrimitereFisier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
                try {
                    salveazaGCode("program_cnc.txt");
                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnDeseneazaLinii);
        buttonPanel.add(btnDeseneazaCurba);
        buttonPanel.add(btnTrimitereFisier);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void adaugaPunct(Point punct) {

        if(puncte.size()>=1)
            if (deseneazaLinii) {
                if(!(puncteLinii.contains(puncte.get(puncte.size() -1))))
                    puncteLinii.add(puncte.get(puncte.size() -1));
                puncteLinii.add(punct);
            } else if (deseneazaCurba) {
                if(!(puncteCurba.contains(puncte.get(puncte.size() -1))))
                    puncteCurba.add(puncte.get(puncte.size() -1));
                puncteCurba.add(punct);
            }
        puncte.add(punct);
    }
    private void salveazaGCode(String numeFisier) throws IOException, InterruptedException {


        StringBuilder string = new StringBuilder(500);
        String newstring = "";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(numeFisier))) {
            //writer.write("G90\n"); // Mod absolut de adresa

            if (puncte.size() >= 2) {
                for (int i = 0; i < puncte.size() - 1; i++) {
                    Point punct1 = puncte.get(i);
                    if(i == 0)
                    {
                        string.append("G0 X" + punct1.x/3 + " Y" + punct1.y/3 + "\n");
                        newstring += "G0 X" + punct1.x/3 + " Y" + punct1.y/3 + "\n";
                        newstring += "D\n";
                        string.append("D\n");
                        // Scrie instrucțiuni G-code pentru mișcarea de la punct1 la punct2
                        writer.write("G0 X" + punct1.x/3 + " Y" + punct1.y/3 + "\n");
                        writer.write("D\n");
                    }
                    Point punct2 = puncte.get(i+1);
                    if(puncteLinii.contains(punct1) && puncteLinii.contains(punct2))
                    {

                        string.append("G1 X" + punct2.x/3 + " Y" + punct2.y/3 + "\n");
                        newstring += "G1 X" + punct2.x/3 + " Y" + punct2.y/3 + "\n";
                        writer.write("G1 X" + punct2.x/3 + " Y" + punct2.y/3 + "\n");
                        //scrie linie
                    }
                    if(puncteCurba.contains(punct1) && puncteCurba.contains(punct2))
                    {
                        int currentY =  1;
                        int R = 1;
                        int currentX =  punct2.x/3;
                        float d = 1/ 4 - R;

                        //Traseaza primul octant al cercului
                        for (int dx = 0 ; dx <= (int)(R / sqrt(2));dx++){
                            //punct2.x/3+ dx, punct2.y/3+ currentY;
                            string.append("G1 X" + punct2.x/3+ dx + " Y" + punct2.y/3+ currentY + "\n");
                            newstring += "G1 X" + punct2.x/3+ dx + " Y" + punct2.y/3+ currentY + "\n";
                            writer.write("G1 X" + punct2.x/3+ dx + " Y" + punct2.y/3+ currentY + "\n");
                            //octagon(renderer,circle,dx, currentY);
                            d += 2 * dx +1;
                            //incrementeaza variabila de decizie cu 2x + 1;
                            if (d > 0)
                            {
                                d += 2-2* currentY;
                                //incrementeaza variabila de decizie cu 2 − 2y;
                                currentY--;
                                //decrementeaza currentY;
                            }
                        }
                        //scrie curba
                    }
                    // Scrie instrucțiuni G-code pentru mișcarea de la punct1 la punct2
//                    string.append("G1 X" + punct2.x/3 + " Y" + punct2.y/3 + "\n");
//                    writer.write("G1 X" + punct2.x/3 + " Y" + punct2.y/3 + "\n");
                }
            }
            string.append("U\n");
            newstring += "U\n";
            newstring += "G0 X0 Y0\n";
            string.append("G0 X0 Y0\n");
            writer.write("U\nG0 X0 Y0\n");
            PrintWriter output = new PrintWriter(sp.getOutputStream());
            if (puncte.size() >= 2) {
                System.out.println("we sent ("+string.length()+") " );

                output.print(string.toString());
                output.flush();
            }


        }
    }
    private void salveazaInFisier(String numeFisier) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(numeFisier))) {
            if (puncte.size() >= 2) {
                for (int i = 0; i < puncte.size() - 1; i++) {
                    Point punct1 = puncte.get(i);
                    Point punct2 = puncte.get(i + 1);

                    writer.write("Linie " + (i + 1) + ": " +
                            "Inceput(" + punct1.x + ", " + punct1.y + "), " +
                            "Final(" + punct2.x + ", " + punct2.y + ")");
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        drawPoints(g);
        drawLine(g);
        drawSemicircle(g);
        //g.drawArc (50, 100 - max(abs(-50 + 100), abs(-100 + 50))/2 - 0/2, max(abs(-50 + 100), abs(-100 + 50)),max(abs(-50 + 100), abs(-100 + 50)), 0,-180 );
        //g.drawArc (250, 250, 200,200, 0,-180 );
        //g.drawArc (250, 250, 200,200, 0,180 );


    }

    private void drawPoints(Graphics g) {
        for (Point punct : puncte) {
            g.fillOval(punct.x - 2, punct.y - 2, 4, 4);
        }
//            g.fillOval(50 - 2, 100 - 2, 4, 4);
//            g.fillOval(100 - 2, 50 - 2, 4, 4);
    }

    private void drawLine(Graphics g) {
        if (puncteLinii.size() >= 2) {
            for (int i = 0; i < puncteLinii.size() - 1; i++) {
                Point punct1 = puncteLinii.get(i);
                Point punct2 = puncteLinii.get(i + 1);
                g.drawLine(punct1.x, punct1.y, punct2.x, punct2.y);
            }
        }
    }

    private void drawArch(Graphics g) {
        if (puncteCurba.size() >= 2) {
            for (int i = 0; i < puncteCurba.size() - 1; i++) {
                Point punct1 = puncteCurba.get(i);
                Point punct2 = puncteCurba.get(i + 1);
                g.setColor(Color.black);

                int archLength = Math.abs(punct1.x - punct2.x);
                int centerX = Math.min(punct1.x, punct2.x) + archLength / 2;
                int centerY = punct1.y;

                double angleRadians = Math.atan2(punct2.y - centerY, punct2.x - centerX);
                double angleDegrees = Math.toDegrees(angleRadians);

                g.drawArc(centerX - archLength / 2, centerY - archLength / 2, archLength, archLength, (int) angleDegrees, 180);
            }
        }
    }
    private void drawSemicircle(Graphics g) {
        if (puncteCurba.size() >= 2) {
            for (int i = 0; i < puncteCurba.size() - 1; i++) {
                Point punct1 = puncteCurba.get(i);
                Point punct2 = puncteCurba.get(i + 1);
                g.setColor(Color.black);

                int semicircleRadius = Math.abs(punct1.x - punct2.x) / 2;
                int centerX = Math.min(punct1.x, punct2.x) + semicircleRadius;
                int centerY = punct1.y;

                g.drawArc(centerX - semicircleRadius, centerY - semicircleRadius, 2 * semicircleRadius, semicircleRadius * 2, 0, ((punct1.x - punct2.x)>0?1:(-1))*180);
            }
        }
    }
    static SerialPort chosenPort;
    static int x = 0;
    public static void main(String[] args) throws IOException, InterruptedException {

        JFrame window = new JFrame();
        window.setTitle("Pentru Port");
        window.setSize(400, 400);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // create a drop-down box and connect button, then place them at the top of the window
        JComboBox<String> portList = new JComboBox<String>();
        JButton connectButton = new JButton("Connect");
        JPanel topPanel = new JPanel();
        topPanel.add(portList);
        topPanel.add(connectButton);
        window.add(topPanel, BorderLayout.NORTH);

        // populate the drop-down box
        SerialPort[] portNames = SerialPort.getCommPorts();
        for(int i = 0; i < portNames.length; i++)
            portList.addItem(portNames[i].getSystemPortName());


        // configure the connect button and use another thread to listen for data
        connectButton.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent arg0) {
                if(connectButton.getText().equals("Connect")) {
                    // attempt to connect to the serial port
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if(chosenPort.openPort()) {
                        connectButton.setText("Disconnect");
                        portList.setEnabled(false);
                    }

                    // create a new thread that listens for incoming text and populates the graph
                    Thread thread = new Thread(){
                        @Override public void run() {
                            System.out.println("connected read");
                            SwingUtilities.invokeLater(() -> {
                                InterfataProiect ex = new InterfataProiect(chosenPort);
                                ex.setVisible(true);
                            });
                        }
                    };
                    thread.start();
                    Thread thread2 = new Thread(){
                        @Override public void run() {
                            Scanner scanner = new Scanner(chosenPort.getInputStream());

                            while(scanner.hasNextLine()) {
                                try {
                                    String line = scanner.nextLine();
                                    System.out.println(line);
                                    window.repaint();
                                } catch(Exception e) {}
                            }
                            scanner.close();
                        }
                    };
                    thread2.start();




                } else {
                    // disconnect from the serial port
                    chosenPort.closePort();
                    portList.setEnabled(true);
                    connectButton.setText("Connect");
                    x = 0;
                }
            }
        });

        // show the window
        window.setVisible(true);



    }
}