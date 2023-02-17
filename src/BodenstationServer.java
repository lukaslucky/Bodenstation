import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class BodenstationServer {

    public String planet;

    private Connection connection;

    // RequestListener-Thread
    public Thread td;

    // Liste aller ChatSessions
    private List<RoboterSession> sessions = null;

    // innere Klasse zum Empfang von Connection-Requests von Clients
    class RequestListener extends Thread {
        private ServerSocket sock;
        private int port;

        public RequestListener(int port) {
            super();
            this.port = port;
        }

        @Override
        public void run() {
            try {
                sock = new ServerSocket(port);
                // SocketTimeout setzen damit sauberes "exit" moeglich
                sock.setSoTimeout(2000);
                while (!Thread.interrupted()) {
                    try {
                        Socket client = sock.accept();
                        // neue Session fuer neuen Client anlegen und eintragen
                        sessions.add(new RoboterSession(BodenstationServer.this, client, connection));
                    } catch (SocketTimeoutException e) {
                    }
                }
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("BodenstationServer ends...");
        }
    }

    public BodenstationServer(int port, Connection connection) {
        this.connection = connection;
        System.out.println("BodenstationServer starts...");
        // synchronisierte Liste erzeugen, da MultiThread-Zugriff auf Liste
        sessions = Collections.synchronizedList(new ArrayList<RoboterSession>());
        // RequestListener-Thread erzeugen und starten
        td = new RequestListener(port);
        td.start();
    }

    public synchronized void sendInit(RoboterSession neu) {
        // Infos ueber neuen Teilnehmer/ bereits angemeldete Teilnehmer
        // verteilen
        neu.send("Roboter " + neu.getSName() + " ist jetzt mit der Bodenstation verbunden");
        // Roboter name in die Datenbank eintragen
        neu.insertRoboterName(neu.getSName());
        ListIterator<RoboterSession> roboterSession = sessions.listIterator();
        while (roboterSession.hasNext()) {
            RoboterSession x = roboterSession.next();
            if (x != neu) {
                neu.send("bereits mit der Bodenstatio verbunden: " + "Roboter-Nummer " + x.getSName());
                x.send("neuer Roboter ist aktiv: " + neu.getSName());
            }
        }

    }

    public synchronized void send2All(String name, String line) {
        // neuen Beitrag mit Name Verfasser an alle verteilen
        for (RoboterSession x : sessions) {
            if (!x.getSName().equals(name)) {
                x.send("Roboter " + name + ": " + line);
            }
        }
    }
    // Planeten name in die Datenbank eintragen
    public void PlanetNameInsert(String name){
        try (PreparedStatement select = connection.prepareStatement("select * from planeten where name = ?");
                PreparedStatement stm = connection.prepareStatement("insert into planeten (name) values (?)")){
            select.setString(1, name);
            ResultSet resultSet = select.executeQuery();
            if (!resultSet.next()){
                stm.setString(1, name);
                stm.executeUpdate();
            }

        } catch (SQLException ex){
            System.err.println(ex);
        }
    }

	/*
	  public synchronized void sendInstruction (String name, String line) {
		for (RoboterSession x : sessions) {
			if (x.getSName().equals(name)) {
				x.send ("Bodenstationsbefehl: " + line);
			}
		}
	}
	*/

    public synchronized void remove(RoboterSession session) {
        // Sitzung austragen
        sessions.remove(session);
    }

    public synchronized void shutdown() {
        // Server shutdown: alle Sitzungen schliessen
        sessions.forEach((x) -> {
            try {
                x.close();
            } catch (Exception e) {
            }
        });
    }


}


