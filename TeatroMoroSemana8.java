

import java.time.LocalDateTime;

import java.util.*;

public class TeatroMoroSemana8 {
    public static void main(String[] args) {

        Theatre theatre = new Theatre(6, 10);


        Event concierto = new Event(1, "Concierto de 31 Minutos1", 12000.0);
        theatre.addEvent(concierto);

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            theatre.purgeExpiredReservations();

            System.out.println("\nTEATRO MORO");
            System.out.println("1) Mostrar Eventos Disponibles");
            System.out.println("2) Venta de Entradas (Asignar Asientos)");
            System.out.println("3) Modificar una Reserva");
            System.out.println("4) Gestionar Clientes (Agregar/Ver)");
            System.out.println("5) Mostrar mapa de Asientos (Evento ID 1)");
            System.out.println("0) Salir");
            System.out.print("Opción: ");
            String opt = sc.nextLine().trim();

            try {
                switch (opt) {
                    case "1" -> theatre.displayEvents();
                    case "2" -> handleSale(theatre, sc);
                    case "3" -> handleModification(theatre, sc);
                    case "4" -> handleClientManagement(theatre, sc);
                    case "5" -> theatre.displaySeats(1); // Muestra solo el evento 1 por simplicidad en el mapa
                    case "0" -> {
                        System.out.println("Saliendo del sistema Tulio.");
                        running = false;
                    }
                    default -> System.out.println("Opción inválida.");
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Entrada numérica inválida. Inténtelo de nuevo.");
            } catch (Exception ex) {
                System.out.println("Error Inesperado: " + ex.getMessage());
            }
        }
        sc.close();
    }

    private static void handleClientManagement(Theatre theatre, Scanner sc) {
        System.out.println(" GESTIÓN ");
        System.out.println("1) Agregar Nuevo Cliente");
        System.out.println("2) Mostrar Todos los Clientes");
        System.out.print("Opción: ");
        String opt = sc.nextLine().trim();

        if (opt.equals("1")) {
            System.out.print("Rut Cliente : ");
            String id = sc.nextLine().trim();
            System.out.print("Nombre: ");
            String name = sc.nextLine().trim();
            System.out.print("Tipo de Cliente (ESTUDIANTE / TERCERA_EDAD / NORMAL): ");
            String typeStr = sc.nextLine().trim().toUpperCase();
            Client.ClientType type;
            try {
                type = Client.ClientType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Tipo inválido. Asignando NORMAL.");
                type = Client.ClientType.NORMAL;
            }
            theatre.addClient(new Client(id, name, type));
            System.out.println("Cliente " + name + " agregado.");
        } else if (opt.equals("2")) {
            theatre.displayClients();
        } else {
            System.out.println("Opción inválida.");
        }
    }

    private static void handleSale(Theatre theatre, Scanner sc) {
        System.out.println("\n VENTA DE ENTRADAS ");
        theatre.displayEvents();
        System.out.print("Ingrese ID del Evento: ");
        int eventId = Integer.parseInt(sc.nextLine().trim());

        Event event = theatre.getEvent(eventId);
        if (event == null) {
            System.out.println("Evento no encontrado.");
            return;
        }

        System.out.print("ID del Cliente : ");
        String clientId = sc.nextLine().trim();
        Client client = theatre.getClient(clientId);
        if (client == null) {
            System.out.println("Cliente no encontrado. Debes registrarlo primero.");
            return;
        }

        theatre.displaySeats(eventId);
        System.out.print("Ingrese asientos a comprar : ");
        String inputSeats = sc.nextLine().trim();
        List<int[]> coords = Theatre.parseSeatList(inputSeats);

        if (coords.isEmpty()) {
            System.out.println("Formato de asiento inválido.");
            return;
        }


        int saleId = theatre.sellSeats(eventId, clientId, coords);
        if (saleId > 0) {
            System.out.println("\n¡Compra exitosa! ID Venta: " + saleId);
            theatre.printSaleSummary(eventId, saleId);
        } else {
            System.out.println("No se pudo completar la venta. Asientos no disponibles o ya vendidos.");
        }
    }

    private static void handleModification(Theatre theatre, Scanner sc) {
        System.out.print("ID del Evento: ");
        int eventId = Integer.parseInt(sc.nextLine().trim());
        System.out.print("ID de la Reserva a modificar: ");
        int resId = Integer.parseInt(sc.nextLine().trim());

        Sale reservation = theatre.getReservation(eventId, resId);
        if (reservation == null || reservation.purchased) {
            System.out.println("Reserva no encontrada, expirada o ya vendida.");
            return;
        }

        theatre.displaySeats(eventId);
        System.out.print("Ingrese NUEVOS asientos (ej: B1,B2): ");
        String newSeats = sc.nextLine().trim();
        List<int[]> coordsNew = Theatre.parseSeatList(newSeats);

        boolean ok = theatre.modifyReservation(eventId, resId, coordsNew);

        if (ok) {
            System.out.println("Reserva modificada con éxito.");
            theatre.printSaleSummary(eventId, resId);
        } else {
            System.out.println("No se pudo modificar la reserva. Verifique disponibilidad.");
        }
    }

    static class Theatre {
        private final Map<Integer, Event> events;
        private final Map<String, Client> clients;
        private final Seat[][] seats;
        private int nextSaleId;

        public Theatre(int rows, int cols) {
            this.events = new HashMap<>();
            this.clients = new HashMap<>();
            this.seats = new Seat[rows][cols];
            this.nextSaleId = 1;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    seats[r][c] = new Seat(r, c);
                }
            }
        }


        public void addClient(Client client) {
            clients.put(client.id, client);
        }

        public Client getClient(String id) {
            return clients.get(id);
        }

        public void displayClients() {
            if (clients.isEmpty()) {
                System.out.println("No hay clientes registrados.");
                return;
            }
            System.out.println("\n--- LISTA DE CLIENTES ---");
            clients.values().forEach(c -> System.out.println("ID: " + c.id + ", Nombre: " + c.name + ", Tipo: " + c.type));
        }


        public void addEvent(Event event) {
            events.put(event.id, event);
        }

        public Event getEvent(int id) {
            return events.get(id);
        }

        public void displayEvents() {
            if (events.isEmpty()) {
                System.out.println("No hay eventos programados.");
                return;
            }
            System.out.println("\n--- EVENTOS DISPONIBLES ---");
            events.values().forEach(e -> System.out.println("ID: " + e.id + ", Nombre: " + e.name + ", Precio Base: $" + e.basePrice));
        }



        public void displaySeats(int eventId) {
            Event event = events.get(eventId);
            if (event == null) {
                System.out.println("Evento no encontrado.");
                return;
            }

            System.out.println("\nMapa de asientos para: " + event.name + " ( $ = libre, R = reservado, X = vendido )");
            System.out.printf("Precio base: $%.0f\n", event.basePrice);
            int rows = seats.length;
            int cols = seats[0].length;


            System.out.print("    ");
            for (int c = 0; c < cols; c++) {
                System.out.printf("%3d", c + 1);
            }
            System.out.println();

            for (int r = 0; r < rows; r++) {
                char rowLabel = (char) ('A' + r);
                System.out.printf("%2s ", rowLabel);
                for (int c = 0; c < cols; c++) {
                    char mark = '.';

                    SeatStatus status = event.getSeatStatus(seatCode(r, c));
                    if (status == SeatStatus.RESERVED) mark = 'R';
                    else if (status == SeatStatus.SOLD) mark = 'X';
                    else mark = '$';
                    System.out.printf("%3c", mark);
                }
                System.out.println();
            }
        }


        public int sellSeats(int eventId, String clientId, List<int[]> coords) {
            Event event = events.get(eventId);
            Client client = clients.get(clientId);
            if (event == null || client == null) return -1;

            for (int[] p : coords) {
                String code = seatCode(p[0], p[1]);
                if (!isValidSeat(p[0], p[1]) || event.getSeatStatus(code) != SeatStatus.AVAILABLE) {
                    System.out.println("Validación Fallida: Asiento " + code + " no disponible o inválido.");
                    return -1;
                }
            }

            int id = nextSaleId++;
            Sale sale = new Sale(id, eventId, clientId, LocalDateTime.now(), client.type);

            double total = 0;
            List<String> seatCodes = new ArrayList<>();
            for (int[] p : coords) {
                String code = seatCode(p[0], p[1]);
                seatCodes.add(code);
                total += event.basePrice;
            }

            sale.seatCodes = seatCodes;
            sale.basePrice = total;


            sale.finalPrice = applyDiscount(total, client.type);
            sale.purchased = true;
            sale.purchasedAt = LocalDateTime.now();


            for(String code : seatCodes) {
                event.updateSeatStatus(code, SeatStatus.SOLD, id);
            }

            event.addSale(sale);
            return id;
        }

        private double applyDiscount(double basePrice, Client.ClientType type) {
            double discount = 0.0;
            if (type == Client.ClientType.ESTUDIANTE) {
                discount = 0.10; // 10%
            } else if (type == Client.ClientType.TERCERA_EDAD) {
                discount = 0.15; // 15%
            }
            return basePrice * (1.0 - discount);
        }


        public int reserveSeats(int eventId, String clientId, List<int[]> coords) {

            return -1;
        }


        public boolean deleteSale(int eventId, int saleId) {
            Event event = events.get(eventId);
            if (event == null) return false;

            Sale sale = event.getSale(saleId);
            if (sale == null) return false;


            for (String code : sale.seatCodes) {
                event.updateSeatStatus(code, SeatStatus.AVAILABLE, 0);
            }

            return event.removeSale(saleId);
        }


        public boolean modifyReservation(int eventId, int resId, List<int[]> newCoords) {
            Event event = events.get(eventId);
            if (event == null) return false;

            Sale res = event.getSale(resId);
            if (res == null || res.purchased) return false;


            for (String code : res.seatCodes) {
                event.updateSeatStatus(code, SeatStatus.AVAILABLE, 0);
            }


            for (int[] p : newCoords) {
                String code = seatCode(p[0], p[1]);
                if (!isValidSeat(p[0], p[1]) || event.getSeatStatus(code) != SeatStatus.AVAILABLE) {
                    return false;
                }
            }

            List<String> newSeatCodes = new ArrayList<>();
            double total = 0;
            for (int[] p : newCoords) {
                String code = seatCode(p[0], p[1]);
                event.updateSeatStatus(code, SeatStatus.RESERVED, resId);
                newSeatCodes.add(code);
                total += event.basePrice;
            }


            res.seatCodes = newSeatCodes;
            res.basePrice = total;
            res.finalPrice = applyDiscount(total, clients.get(res.clientId).type);
            res.modifiedAt = LocalDateTime.now(); // reinicia el TTL
            return true;
        }

        public Sale getReservation(int eventId, int saleId) {
            Event event = events.get(eventId);
            return event != null ? event.getSale(saleId) : null;
        }

        public void printSaleSummary(int eventId, int saleId) {
            Event event = events.get(eventId);
            Sale sale = event != null ? event.getSale(saleId) : null;
            Client client = clients.get(sale.clientId);

            if (sale == null || event == null || client == null) {
                System.out.println("Venta/Reserva no encontrada.");
                return;
            }

            System.out.println("\n--- DETALLE DE VENTA/RESERVA " + saleId + " ---");
            System.out.println("Evento: " + event.name);
            System.out.println("Cliente: " + client.name + " (" + client.type + ")");
            System.out.println("Asientos: " + String.join(", ", sale.seatCodes));
            System.out.printf("Precio Base: $%.0f\n", sale.basePrice);
            if (sale.basePrice != sale.finalPrice) {
                System.out.printf("Descuento Aplicado: %.0f%%\n", (1 - sale.finalPrice / sale.basePrice) * 100);
            }
            System.out.printf("Precio Final: $%.0f\n", sale.finalPrice);
            System.out.println("Estado: " + (sale.purchased ? "VENDIDO" : "RESERVADO"));
            System.out.println("----------------------------------------");
        }

        public void purgeExpiredReservations() {

        }


        private boolean isValidSeat(int r, int c) {
            return r >= 0 && r < seats.length && c >= 0 && c < seats[0].length;
        }

        private String seatCode(int r, int c) {
            char row = (char) ('A' + r);
            return row + String.valueOf(c + 1);
        }

        public static List<int[]> parseSeatList(String input) {
            List<int[]> list = new ArrayList<>();
            if (input == null || input.isBlank()) return list;
            String[] parts = input.split(",");
            for (String p : parts) {
                p = p.trim().toUpperCase();
                if (p.matches("^[A-Z]\\d+$")) {
                    list.add(parseSingleSeat(p));
                }
            }
            return list;
        }

        public static int[] parseSingleSeat(String code) {
            code = code.trim().toUpperCase();
            char r = code.charAt(0);
            int row = r - 'A';
            int col;
            try {
                col = Integer.parseInt(code.substring(1)) - 1;
            } catch (NumberFormatException e) {
                col = -1;
            }
            return new int[]{row, col};
        }
    }

    static class Seat {
        int row, col;
        public Seat(int r, int c) {
            this.row = r;
            this.col = c;
        }
    }

    enum SeatStatus { AVAILABLE, RESERVED, SOLD }

    static class Client {
        enum ClientType { ESTUDIANTE, TERCERA_EDAD, NORMAL }
        String id;
        String name;
        ClientType type;

        public Client(String id, String name, ClientType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    static class Sale {
        int id;
        int eventId;
        String clientId;
        List<String> seatCodes;
        double basePrice;
        double finalPrice;
        boolean purchased;
        LocalDateTime createdAt;
        LocalDateTime modifiedAt;
        LocalDateTime purchasedAt;

        public Sale(int id, int eventId, String clientId, LocalDateTime created, Client.ClientType clientType) {
            this.id = id;
            this.eventId = eventId;
            this.clientId = clientId;
            this.createdAt = created;
            this.seatCodes = new ArrayList<>();
        }
    }

    static class Event {
        int id;
        String name;
        double basePrice;

        private final Map<String, SeatInfo> seatMap;
        private final Map<Integer, Sale> sales;
        public Event(int id, String name, double basePrice) {
            this.id = id;
            this.name = name;
            this.basePrice = basePrice;
            this.seatMap = new HashMap<>();
            this.sales = new HashMap<>();
        }

        private SeatInfo getSeatInfo(String code) {
            return seatMap.getOrDefault(code, new SeatInfo(SeatStatus.AVAILABLE, 0));
        }

        public SeatStatus getSeatStatus(String code) {
            return getSeatInfo(code).status;
        }

        public void updateSeatStatus(String code, SeatStatus status, int saleId) {
            seatMap.put(code, new SeatInfo(status, saleId));
        }

        public void addSale(Sale sale) {
            sales.put(sale.id, sale);
        }

        public Sale getSale(int id) {
            return sales.get(id);
        }

        public boolean removeSale(int id) {
            return sales.remove(id) != null;
        }

        private static class SeatInfo {
            SeatStatus status;
            int saleId;
            public SeatInfo(SeatStatus status, int saleId) {
                this.status = status;
                this.saleId = saleId;
            }
        }
    }
}