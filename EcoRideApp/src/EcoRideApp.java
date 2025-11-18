import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

//---------------------------------------------------------
// ENUMS
//---------------------------------------------------------
enum KU00153751_Category {
    COMPACT_PETROL(5000, 100, 50, 0.10),
    HYBRID(7500, 150, 60, 0.12),
    ELECTRIC(10000, 200, 40, 0.08),
    LUXURY_SUV(15000, 250, 75, 0.15);

    public final int dailyRate;
    public final int freeKmPerDay;
    public final int extraKmCharge;
    public final double taxRate;

    KU00153751_Category(int dailyRate, int freeKmPerDay, int extraKmCharge, double taxRate) {
        this.dailyRate = dailyRate;
        this.freeKmPerDay = freeKmPerDay;
        this.extraKmCharge = extraKmCharge;
        this.taxRate = taxRate;
    }
}

enum KU00153751_AvailabilityStatus {
    AVAILABLE,
    RESERVED,
    UNDER_MAINTENANCE
}

//---------------------------------------------------------
// MODEL CLASSES
//---------------------------------------------------------
// VEHICLE CLASS//

class KU00153751_Vehicle {
    String carId;
    String model;
    KU00153751_Category category;
    KU00153751_AvailabilityStatus status;

    KU00153751_Vehicle(String carId, String model, KU00153751_Category category) {
        this.carId = carId;
        this.model = model;
        this.category = category;
        this.status = KU00153751_AvailabilityStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return carId + " | " + model + " | " + category +
                " | Rate: " + category.dailyRate + " | " + status;
    }
}

class KU00153751_Customer {
    String idNumber;
    String name;
    String contact;
    String email;

    KU00153751_Customer(String idNumber, String name, String contact, String email) {
        this.idNumber = idNumber;
        this.name = name;
        this.contact = contact;
        this.email = email;
    }

    @Override
    public String toString() {
        return name + " (" + idNumber + ") - " + contact;
    }
}

class KU00153751_Reservation {
    String bookingId;
    KU00153751_Customer customer;
    KU00153751_Vehicle vehicle;
    LocalDate bookingDate;
    LocalDate rentalStart;
    int days;
    int expectedKm;
    boolean depositTaken;

    KU00153751_Reservation(KU00153751_Customer customer, KU00153751_Vehicle vehicle,
                           LocalDate bookingDate, LocalDate rentalStart,
                           int days, int expectedKm) {
        this.bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.customer = customer;
        this.vehicle = vehicle;
        this.bookingDate = bookingDate;
        this.rentalStart = rentalStart;
        this.days = days;
        this.expectedKm = expectedKm;
        this.depositTaken = true;
    }

    @Override
    public String toString() {
        return "BookingID:" + bookingId + " | " + customer.name + " | Car:" + vehicle.carId
                + " | Start:" + rentalStart + " | Days:" + days + " | ExpKm:" + expectedKm;
    }
}

class KU00153751_Invoice {
    double basePrice, extraKm, discount, tax, deposit, finalPayable;

    KU00153751_Invoice(double basePrice, double extraKm, double discount, double tax, double deposit) {
        this.basePrice = basePrice;
        this.extraKm = extraKm;
        this.discount = discount;
        this.tax = tax;
        this.deposit = deposit;
        this.finalPayable = basePrice + extraKm - discount + tax - deposit;
    }

    @Override
    public String toString() {
        return
                "Base Price: LKR " + basePrice +
                        "\nExtra KM Charges: LKR " + extraKm +
                        "\nDiscount: LKR " + discount +
                        "\nTax: LKR " + tax +
                        "\nDeposit Deducted: LKR " + deposit +
                        "\n----------------------------------" +
                        "\nFinal Payable: LKR " + finalPayable;
    }
}

//---------------------------------------------------------
// SYSTEM CONTROLLER
//---------------------------------------------------------
class KU00153751_EcoRideSystem {

    public static final int REFUNDABLE_DEPOSIT = 5000;

    Map<String, KU00153751_Vehicle> vehicles = new HashMap<>();
    Map<String, KU00153751_Customer> customers = new HashMap<>();
    Map<String, KU00153751_Reservation> reservations = new HashMap<>();

    // Add Vehicle
    boolean addVehicle(String id, String model, KU00153751_Category category) {
        if (vehicles.containsKey(id)) return false;
        vehicles.put(id, new KU00153751_Vehicle(id, model, category));
        return true;
    }

    // Update Vehicle
    boolean updateVehicle(String id, String model, KU00153751_Category category,
                          KU00153751_AvailabilityStatus status) {
        KU00153751_Vehicle v = vehicles.get(id);
        if (v == null) return false;
        v.model = model;
        v.category = category;
        v.status = status;
        return true;
    }

    // Remove Vehicle
    boolean removeVehicle(String id) {
        KU00153751_Vehicle v = vehicles.get(id);
        if (v == null) return false;
        for (KU00153751_Reservation r : reservations.values()) {
            if (r.vehicle.carId.equals(id))
                return false;
        }
        vehicles.remove(id);
        return true;
    }

    // Register Customer
    boolean registerCustomer(String id, String name, String contact, String email) {
        if (customers.containsKey(id)) return false;
        customers.put(id, new KU00153751_Customer(id, name, contact, email));
        return true;
    }

    // Make Reservation
    KU00153751_Reservation makeReservation(String custId, String carId,
                                           LocalDate today, LocalDate start,
                                           int days, int expectedKm) throws Exception {

        KU00153751_Customer cust = customers.get(custId);
        KU00153751_Vehicle veh = vehicles.get(carId);

        if (cust == null) throw new Exception("Customer not found.");
        if (veh == null) throw new Exception("Vehicle not found.");
        if (veh.status != KU00153751_AvailabilityStatus.AVAILABLE)
            throw new Exception("Vehicle not available.");

        long gap = ChronoUnit.DAYS.between(today, start);
        if (gap < 3) throw new Exception("Booking must be at least 3 days before rental.");

        KU00153751_Reservation r = new KU00153751_Reservation(
                cust, veh, today, start, days, expectedKm
        );

        reservations.put(r.bookingId, r);
        veh.status = KU00153751_AvailabilityStatus.RESERVED;
        return r;
    }

    // Search Reservation
    List<KU00153751_Reservation> search(String query) {
        List<KU00153751_Reservation> out = new ArrayList<>();
        String q = query.toLowerCase();
        for (KU00153751_Reservation r : reservations.values()) {
            if (r.bookingId.toLowerCase().contains(q) || r.customer.name.toLowerCase().contains(q))
                out.add(r);
        }
        return out;
    }

    // Bookings by rental date
    List<KU00153751_Reservation> bookingsBy(LocalDate date) {
        List<KU00153751_Reservation> out = new ArrayList<>();
        for (KU00153751_Reservation r : reservations.values()) {
            if (r.rentalStart.equals(date)) out.add(r);
        }
        return out;
    }

    // Finalize invoice
    KU00153751_Invoice finalizeInvoice(String bookingId, int actualKm) throws Exception {
        KU00153751_Reservation r = reservations.get(bookingId);
        if (r == null) throw new Exception("Reservation not found.");

        KU00153751_Category c = r.vehicle.category;

        double base = c.dailyRate * r.days;

        int allowed = c.freeKmPerDay * r.days;
        double extra = actualKm > allowed ? (actualKm - allowed) * c.extraKmCharge : 0;

        double disc = r.days >= 7 ? base * 0.10 : 0;

        double taxable = (base - disc + extra);
        double tax = taxable * c.taxRate;

        KU00153751_Invoice invoice = new KU00153751_Invoice(
                base, extra, disc, tax, REFUNDABLE_DEPOSIT
        );

        r.vehicle.status = KU00153751_AvailabilityStatus.AVAILABLE;
        reservations.remove(bookingId);

        return invoice;
    }

    // Cancel Reservation
// Returns true if cancelled successfully; false if not found.
// Throws Exception if cancellation is not allowed (e.g., on/after rental start)
    public boolean cancelReservation(String bookingId, LocalDate today) throws Exception {
        KU00153751_Reservation r = reservations.get(bookingId);
        if (r == null) {
            return false; // not found
        }

        // Only allow cancellation before the rental start date
        if (!today.isBefore(r.rentalStart)) {
            throw new Exception("Cannot cancel reservation on or after rental start date.");
        }

        // Free the vehicle and remove the reservation
        r.vehicle.status = KU00153751_AvailabilityStatus.AVAILABLE;
        reservations.remove(bookingId);
        return true;
    }

}

//---------------------------------------------------------
// MAIN APPLICATION (MENU)
//---------------------------------------------------------
public class EcoRideApp {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        KU00153751_EcoRideSystem sys = new KU00153751_EcoRideSystem();

        // Default sample data


        while (true) {
            System.out.println("\n========= EcoRide Car Rental System =========");
            System.out.println("1. Register Customer");
            System.out.println("2. Add Vehicle");
            System.out.println("3. Update Vehicle");
            System.out.println("4. Remove Vehicle");
            System.out.println("5. List Vehicles");
            System.out.println("6. Make Reservation (book car)");
            System.out.println("7. Search Reservation");
            System.out.println("8. View Bookings by Date");
            System.out.println("9. Generate / Finalize Invoice");
            System.out.println("10. List All Reservations");
            System.out.println("11. Cancel Reservation");
            System.out.println("0. Exit");
            System.out.print("Choice > ");

            int choice = Integer.parseInt(sc.nextLine());

            try {

                switch (choice) {

                    case 1 -> {
                        System.out.print("NIC/Passport: ");
                        String id = sc.nextLine();
                        System.out.print("Name: ");
                        String name = sc.nextLine();
                        System.out.print("Contact: ");
                        String contact = sc.nextLine();
                        System.out.print("Email: ");
                        String email = sc.nextLine();

                        System.out.println(
                                sys.registerCustomer(id, name, contact, email)
                                        ? "Customer Registered."
                                        : "Customer Already Exists."
                        );
                    }

                    case 2 -> {
                        System.out.print("Car ID: ");
                        String id = sc.nextLine();
                        System.out.print("Model: ");
                        String model = sc.nextLine();

                        KU00153751_Category cat = selectCategory(sc);

                        System.out.println(
                                sys.addVehicle(id, model, cat)
                                        ? "Vehicle Added."
                                        : "Vehicle Already Exists."
                        );
                    }

                    case 3 -> {
                        System.out.print("Car ID: ");
                        String id = sc.nextLine();

                        System.out.print("New Model: ");
                        String model = sc.nextLine();

                        KU00153751_Category cat = selectCategory(sc);
                        KU00153751_AvailabilityStatus st = selectStatus(sc);

                        System.out.println(
                                sys.updateVehicle(id, model, cat, st)
                                        ? "Updated."
                                        : "Vehicle Not Found."
                        );
                    }

                    case 4 -> {
                        System.out.print("Car ID: ");
                        String id = sc.nextLine();

                        System.out.println(
                                sys.removeVehicle(id)
                                        ? "Vehicle Removed."
                                        : "Cannot Remove (Reserved or Not Found)."
                        );
                    }

                    case 5 -> {
                        for (KU00153751_Vehicle v : sys.vehicles.values()) {
                            System.out.println(v);
                        }
                    }

                    case 6 -> {
                        System.out.print("Customer ID: ");
                        String cid = sc.nextLine();

                        System.out.print("Car ID: ");
                        String vid = sc.nextLine();

                        LocalDate today = LocalDate.now();
                        System.out.println("Today: " + today);

                        System.out.print("Rental Start (YYYY-MM-DD): ");
                        LocalDate start = LocalDate.parse(sc.nextLine());

                        System.out.print("Days: ");
                        int days = Integer.parseInt(sc.nextLine());

                        System.out.print("Expected Total KM: ");
                        int km = Integer.parseInt(sc.nextLine());

                        KU00153751_Reservation r = sys.makeReservation(
                                cid, vid, today, start, days, km
                        );

                        System.out.println("Reservation Successful! Booking ID: " + r.bookingId);
                    }

                    case 7 -> {
                        System.out.print("Search Query: ");
                        String q = sc.nextLine();
                        List<KU00153751_Reservation> res = sys.search(q);
                        for (KU00153751_Reservation rr : res) System.out.println(rr);
                    }

                    case 8 -> {
                        System.out.print("Rental Date (YYYY-MM-DD): ");
                        LocalDate d = LocalDate.parse(sc.nextLine());
                        List<KU00153751_Reservation> list = sys.bookingsBy(d);
                        for (KU00153751_Reservation rr : list) System.out.println(rr);
                    }

                    case 9 -> {
                        System.out.print("Booking ID: ");
                        String bid = sc.nextLine().toUpperCase();
                        System.out.print("Actual KM Used: ");
                        int km = Integer.parseInt(sc.nextLine());

                        KU00153751_Invoice invoice = sys.finalizeInvoice(bid, km);
                        System.out.println("\n--- INVOICE ---");
                        System.out.println(invoice);
                    }

                    case 10 -> {
                        for (KU00153751_Reservation rr : sys.reservations.values())
                            System.out.println(rr);
                    }

                    case 11 -> {
                        System.out.print("Booking ID to cancel: ");
                        String bid = sc.nextLine().toUpperCase();
                        LocalDate today = LocalDate.now();
                        try {
                            boolean cancelled = sys.cancelReservation(bid, today);
                            if (cancelled) {
                                System.out.println("Reservation cancelled successfully.");
                            } else {
                                System.out.println("Reservation not found.");
                            }
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                    case 0 -> {
                        System.out.println("Goodbye!");
                        return;
                    }

                    default -> System.out.println("Invalid Choice.");
                }

            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }

        }
    }

    private static KU00153751_Category selectCategory(Scanner sc) {
        System.out.println("1. Compact Petrol");
        System.out.println("2. Hybrid");
        System.out.println("3. Electric");
        System.out.println("4. Luxury SUV");
        System.out.print("Choose > ");
        return switch (sc.nextLine()) {
            case "1" -> KU00153751_Category.COMPACT_PETROL;
            case "2" -> KU00153751_Category.HYBRID;
            case "3" -> KU00153751_Category.ELECTRIC;
            case "4" -> KU00153751_Category.LUXURY_SUV;
            default -> KU00153751_Category.COMPACT_PETROL;
        };
    }

    private static KU00153751_AvailabilityStatus selectStatus(Scanner sc) {
        System.out.println("1. AVAILABLE");
        System.out.println("2. RESERVED");
        System.out.println("3. UNDER MAINTENANCE");
        System.out.print("Choose > ");
        return switch (sc.nextLine()) {
            case "1" -> KU00153751_AvailabilityStatus.AVAILABLE;
            case "2" -> KU00153751_AvailabilityStatus.RESERVED;
            case "3" -> KU00153751_AvailabilityStatus.UNDER_MAINTENANCE;
            default -> KU00153751_AvailabilityStatus.AVAILABLE;
        };
    }
}
