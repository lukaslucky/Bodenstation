import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:/Users/Lukas.Geiser/Desktop/Projekt Java/code/Bodenstation/identifier.sqlite");) {
            // ChatServer anlegen
            BodenstationServer bs = new BodenstationServer(9999, connection);
            // Admin-Console zum shutdown anlegen
            Scanner in = new Scanner(System.in);
            String s;
            System.out.println("welcher Planet wird erkundet:");

            while ((s = in.nextLine()) != null) {
                if (s.equalsIgnoreCase("exit")) {
                    bs.td.interrupt();
                    bs.shutdown();
                    break;
                } else {
                    bs.planet = s;
                    bs.PlanetNameInsert(bs.planet);
                }

			/*
			 String[] inputParts = s.split(":+");
			if (inputParts.length != 2) {
				System.out.print("fehlerhafte Eingabe");
				continue;
			}
			String command = inputParts[0];
			String robotername = inputParts[1];
			var commands = List.of("orbit","scan","move","mvscan","rotate","getpos","charge");

			if (commands.contains(command)) {
				bs.sendInstruction (robotername, command);
			}
			*/
            }

            in.close();
        }
    }
}
