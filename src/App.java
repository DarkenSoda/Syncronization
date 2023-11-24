import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Network {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.println("What is the number of WI-FI Connections?");
        int maxConnections = sc.nextInt();

        System.out.println("What is the number of devices Clients want to connect?");
        int totalDevices = sc.nextInt();
        sc.nextLine();

        Router router = new Router(maxConnections);
        List<Device> devices = new ArrayList<>();

        Type[] types = Type.values();
        for (int i = 0; i < totalDevices; i++) {
            System.out.printf("Enter device %d Name:\n", i + 1);
            String name = sc.nextLine();

            System.out.printf("Device %d Type:\n1) Phone\n2) Laptop\n3) PC\n", i + 1);
            int type = sc.nextInt() - 1;
            sc.nextLine();

            if (type < 0 || type >= types.length) {
                System.out.println("Invalid Type - defaulting to Phone..");
                type = 0;
            }

            Device device = new Device(name, types[type], router);
            devices.add(device);
        }

        LogSaver.clearFile();
        for (Device device : devices) {
            device.start();
        }

        sc.close();
    }
}

class Semaphore {
    private int value = 0;

    public Semaphore() {
        value = 0;
    }

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void acquire(Device device) throws InterruptedException {
        value--;
        if (value < 0) {
            try {
                String text = "- (" + device.get_Name() + ") (" + device.getType() + ") Arrived and Waiting";
                System.out.println(text);
                LogSaver.appendFile(text + '\n');
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        } else {
            String text = "- (" + device.get_Name() + ") (" + device.getType() + ") Arrived";
            System.out.println(text);
            LogSaver.appendFile(text + '\n');
        }
    }

    public synchronized void release() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}

class Router {
    private int routerCapacity;
    private int connectionCount;
    private Semaphore semaphore;
    private Device[] connectedDevices;

    public Router(int routerCapacity) {
        this.routerCapacity = routerCapacity;
        connectedDevices = new Device[routerCapacity];
        semaphore = new Semaphore(routerCapacity);
        connectionCount = 0;
    }

    public void connectDevice(Device device) throws InterruptedException {
        semaphore.acquire(device);

        if (connectionCount < routerCapacity) {
            addDevice(device);
            String text = "- Connection " + getDeviceOrder(device) + ": " + device.get_Name() + " Occupied";
            System.out.println(text);
            LogSaver.appendFile(text + '\n');
        }
    }

    public void disconnectDevice(Device device) {
        removeDevice(device);
        semaphore.release();
    }

    public int getDeviceOrder(Device device) {
        for (int i = 0; i < connectedDevices.length; i++) {
            if (connectedDevices[i] == device)
                return i + 1;
        }

        return -1;
    }

    private void addDevice(Device device) {
        for (int i = 0; i < connectedDevices.length; i++) {
            if (connectedDevices[i] == null) {
                connectionCount++;
                connectedDevices[i] = device;
                return;
            }
        }
    }

    private void removeDevice(Device device) {
        for (int i = 0; i < connectedDevices.length; i++) {
            if (connectedDevices[i] == device) {
                connectionCount--;
                connectedDevices[i] = null;
                return;
            }
        }
    }
}

class Device extends Thread {
    private String name;
    private Type type;
    private Router router;

    public Device(String name, Type type, Router router) {
        this.name = name;
        this.type = type;
        this.router = router;
    }

    @Override
    public void run() {
        try {
            router.connectDevice(this);
            connect();

            Thread.sleep((int) (Math.random() * 1500));
            performOnlineActivity();
            Thread.sleep((int) (Math.random() * 1500));

            logOut();
            router.disconnectDevice(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        String text = "- Connection " + router.getDeviceOrder(this) + ": " + name + " Login";
        System.out.println(text);
        LogSaver.appendFile(text + '\n');
    }

    public void performOnlineActivity() {
        String text = "- Connection " + router.getDeviceOrder(this) + ": " + name
                + " performs online activity";
        System.out.println(text);
        LogSaver.appendFile(text + '\n');

    }

    public void logOut() {
        String text = "- Connection " + router.getDeviceOrder(this) + ": " + name + " Logged out";
        System.out.println(text);
        LogSaver.appendFile(text + '\n');
    }

    // #region Getters
    public String get_Name() {
        return name;
    }

    public Type getType() {
        return type;
    }
    // #endregion
}

enum Type {
    Mobile, Tablet, PC
}

/**
 * The LogSaver is a Singleton class.
 * provides methods to clear and append text to a log file.
 */
class LogSaver {
    private static final String fileName = "log.txt";

    private LogSaver() {
    }

    public static void clearFile() {
        try (FileWriter fileWriter = new FileWriter(fileName, false)) {
            fileWriter.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendFile(String text) {
        try (FileWriter fileWriter = new FileWriter(fileName, true)) {
            fileWriter.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}