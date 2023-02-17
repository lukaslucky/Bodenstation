import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


public class RoboterSession extends Thread {

    static DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private Connection connection;
    public List<String> alles = new ArrayList<>();
    // Socket mit zugehoerigen Streams
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    // Name des Clients
    private String name;

    private BodenstationServer bs;

    public RoboterSession(BodenstationServer bs, Socket client, Connection connection) {
        super();
        this.client = client;
        this.bs = bs;
        this.connection = connection;
        try {
            in = new BufferedReader(new InputStreamReader(
                    client.getInputStream()));
            // erste Zeile als Teilnehmername verwenden
            name = in.readLine();
            out = new PrintWriter(client.getOutputStream(), true);
            // neuen Teilnehmer begruessen/ bekannt machen
            bs.sendInit(this);
            // ChatSession-Thread starten
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // neue Beitraege dieser Session einlesen
        try {
            String line;

            int x = 0;
            int y = 0;
            String direction = "";
            String ground = "";
            int temp = 0;

            while (((line = in.readLine()) != null)) {

                String[] inputParts = line.split(":");
                String command = inputParts[0];
                String[] inputPartsC;
                String[] inputPartsl;

                if (Objects.equals(command, "init")) {
                    inputPartsC = inputParts[1].split(Pattern.quote("|"));
                    int xC = Integer.parseInt(inputPartsC[1]);
                    int yC = Integer.parseInt(inputPartsC[2]);

                    RoboterSession.this.updatePlanet(xC, yC, bs.planet);
                } else if (Objects.equals(command, "land")) {
                    inputPartsl = inputParts[1].split(Pattern.quote("|"));
                    x = Integer.parseInt(inputPartsl[1]);
                    y = Integer.parseInt(inputPartsl[2]);
                    direction = inputPartsl[3];
                } else if (Objects.equals(command, "landed")) {
                    inputPartsl = inputParts[1].split(Pattern.quote("|"));
                    ground = inputPartsl[1];
                    temp = (int) Double.parseDouble(inputPartsl[2]);

                    RoboterSession.this.insertFeld(x, y, bs.planet, ground);
                    RoboterSession.this.insertTemperatur(temp, name, bs.planet, x, y);
                    RoboterSession.this.updateRoboterfeld(bs.planet, name, direction, x, y);
                } else if (Objects.equals(command, "scaned")) {
                    inputPartsC = inputParts[1].split(Pattern.quote("|"));
                    ground = inputPartsC[1];
                    temp = (int) Double.parseDouble(inputPartsC[2]);
                    int xF = 0;
                    int yF = 0;

                    if (Objects.equals(direction, "NORTH")) {
                        xF = x;
                        yF = y - 1;
                    } else if (Objects.equals(direction, "EAST")) {
                        xF = x + 1;
                        yF = y;
                    } else if (Objects.equals(direction, "SOUTH")) {
                        xF = x - 1;
                        yF = y;
                    } else if (Objects.equals(direction, "WEST")) {
                        xF = x;
                        yF = y + 1;
                    }
                    RoboterSession.this.insertFeld(xF, yF, bs.planet, ground);
                    RoboterSession.this.insertTemperatur(temp, name, bs.planet, xF, yF);
                } else if (Objects.equals(command, "moved")) {
                    inputPartsl = inputParts[1].split(Pattern.quote("|"));
                    x = Integer.parseInt(inputPartsl[1]);
                    y = Integer.parseInt(inputPartsl[2]);
                    direction = inputPartsl[3];

                    RoboterSession.this.updateRoboterfeld(bs.planet, name, direction, x, y);
                } else if (Objects.equals(command, "mvscaned")) {
                    inputPartsC = inputParts[1].split(Pattern.quote("|"));
                    ground = inputPartsC[1];
                    temp = (int) Double.parseDouble(inputPartsC[2]);
                    inputPartsl = inputParts[2].split(Pattern.quote("|"));
                    x = Integer.parseInt(inputPartsl[1]);
                    y = Integer.parseInt(inputPartsl[2]);
                    direction = inputPartsl[3];

                    RoboterSession.this.insertFeld(x, y, bs.planet, ground);
                    RoboterSession.this.insertTemperatur(temp, name, bs.planet, x, y);
                    RoboterSession.this.updateRoboterfeld(bs.planet, name, direction, x, y);
                } else if (Objects.equals(command, "rotated")) {
                    direction = inputParts[1];

                    RoboterSession.this.updateRoboterfeld(bs.planet, name, direction, x, y);
                }


                alles.add(line);
                System.out.println(name + ":" + line);
                //bs.send2All(name, line);
            }
        } catch (IOException e) {

        }
        // Nach Verbindungsabbruch Sitzung austragen und
        bs.remove(this);
        // andere Sessions benachrichtigen
        bs.send2All(name, "verbindung ist abgebrochen");

        System.out.println("RoboterSession run ends");
    }

    public void send(String line) {
        // senden Nachricht an zugehoerigen Client
        out.println(line);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
        }
    }

    public String getSName() {
        return name;
    }

    public void insertRoboterName(String name) {
        try (PreparedStatement select = connection.prepareStatement("select * from roboter where name = ?");
             PreparedStatement stm = connection.prepareStatement("insert into roboter (name) values (?)")) {
            select.setString(1, name);
            ResultSet resultSet = select.executeQuery();
            if (!resultSet.next()) {
                stm.setString(1, name);
                stm.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(ex);
        }

    }

    public void updateRoboterfeld(String planet, String roboter, String direction, int x, int y) {
        try (PreparedStatement stm = connection.prepareStatement("update roboter set planet = ?, position_x = ?, position_y = ?, direction = ? where name = ?")) {
            stm.setString(1, planet);
            stm.setString(5, roboter);
            stm.setString(4, direction);
            stm.setInt(2, x);
            stm.setInt(3, y);
            stm.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex);
        }

    }

    public void updatePlanet(int x, int y, String planet) {
        try (PreparedStatement stm = connection.prepareStatement("update planeten set size_x = ?, size_y = ? where name = ?")) {
            stm.setInt(1, x);
            stm.setInt(2, y);
            stm.setString(3, planet);
            stm.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex);
        }

    }

    public void insertTemperatur(int temperatur, String roboter, String planet, int x, int y) {
        try (PreparedStatement stm = connection.prepareStatement("insert into temperatur (temp, roboter, planet, x, y, timestamp) values (?,?,?,?,?,?);")) {
            LocalDateTime timestamp = LocalDateTime.now();
            stm.setInt(1, temperatur);
            stm.setString(2, roboter);
            stm.setString(3, planet);
            stm.setInt(4, x);
            stm.setInt(5, y);
            stm.setString(6, timestamp.format(ISO_FORMATTER));
            stm.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex);
        }

    }

    public void insertFeld(int x, int y, String planet, String ground) {
        try (PreparedStatement select = connection.prepareStatement("select * from felder where planet = ? and x  = ? and y = ?");
             PreparedStatement stm = connection.prepareStatement("insert into felder (planet,x,y,ground) values (?,?,?,?)")) {
            select.setString(1, planet);
            select.setInt(2, x);
            select.setInt(3, y);
            ResultSet resultSet = select.executeQuery();
            if (!resultSet.next()) {
                stm.setString(1, planet);
                stm.setInt(2, x);
                stm.setInt(3, y);
                stm.setString(4, ground);
                stm.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(ex);
        }

    }
}



