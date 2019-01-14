import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

public class Serwer implements Runnable
{
    Socket csocket;
    static String AdresBazyDanych="192.168.0.15";
    static String NazwaBazyDanych="PWJ_Projekt";
    static String NazwaUzytkownika="PWJ";
    static String HasłoDoBazy="asdf";
    String TypZalogowanego="";
    String ObecnieZalogowany="";
    Serwer(Socket csocket)
    {
        this.csocket = csocket;
    }

    static boolean ladujSterownik()
    {
        // LADOWANIE STEROWNIKA
        System.out.print("Sprawdzanie sterownika:");
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            return true;
        } catch (Exception e) {
            System.out.println("Blad przy ladowaniu sterownika bazy!");
            return false;
        }
    }

    private static Connection getConnection(String adres, int port) {

        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", NazwaUzytkownika);
        connectionProps.put("password", HasłoDoBazy);

        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + adres + ":" + port + "/",
                    connectionProps);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    private static Connection connectToDatabase(String adress,
                                                String dataBaseName, String userName, String password)
    {
        String baza = "jdbc:mysql://" + adress + "/" + dataBaseName;
        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(baza, userName, password);
            System.out.println("Polaczono z baza");
        } catch (SQLException e) {
            System.out.println("Nie polaczono z baza!");
            /**
             * utworzenie bazy w przypadku gdy jej nie wykryto
             */
            try
            {
                Connection con=getConnection(adress,3306);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+dataBaseName+";") > -1)
                    System.out.println("Baza wybrana");
                else
                {
                    if (executeUpdate(st, "create Database "+dataBaseName+";") > -1)
                        System.out.println("Baza utworzona");
                    else
                        System.out.println("Baza niewybrana!");
                    connection=connectToDatabase(adress,dataBaseName,userName,password);
                }
            }
            catch (Exception e1)
            {
                connection=connectToDatabase(adress,dataBaseName,userName,password);
            }
        }
        Statement st = createStatement(connection);
        //Sprawdzenie czy tabele istnieja, a jesli nie to utworzenie ich
        try
        {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "Uzytkownicy", null);
            if (tables.next()) {
                System.out.println("Tabela Uzytkownicy istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Uzytkownicy (login VARCHAR(50) unique NOT NULL, haslo VARCHAR(50) NOT NULL, Imie VARCHAR(50) NOT NULL, Nazwisko VARCHAR(50) NOT NULL, Email VARCHAR(50) unique NOT NULL, typ enum(\"student\",\"prowadzacy\",\"administrator\") NOT NULL, Prowadzone_przedmioty VARCHAR(250), Uczeszczane_przedmioty VARCHAR(250), CzyZatwierdzony tinyint(1) not null);") > -1)
                {
                    System.out.println("Tabela Uzytkownicy utworzona");
                    // utworzenie zatwierdzonego administratora
                    executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ ,CzyZatwierdzony ) VALUES ('administrator', 'administrator', 'administrator', 'administrator', 'administrator@admin.pl','administrator', 1); ");
                }
                else
                    System.out.println("Tabela Uzytkownicy nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Przedmioty", null);
            if (tables.next()) {
                System.out.println("Tabela Przedmioty istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Przedmioty (Nazwa VARCHAR(50) unique NOT NULL, Nazwisko_prowadzacego VARCHAR(50) NOT NULL, Preferowany_czas_prowadzacego VARCHAR(150), Godziny_przedmiotu VARCHAR(50), Uczeszczajacy VARCHAR(500) );") > -1)
                    System.out.println("Tabela Przedmioty utworzona");
                else
                    System.out.println("Tabela Przedmioty nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Serwery", null);
            Statement st1 = createStatement(connection);
            String ip=InetAddress.getLocalHost().toString();
            String []ipRozlozone=ip.split("/");
            ip=ipRozlozone[1];
            if (tables.next()) {
                System.out.println("Tabela Serwery istnieje");
                //sprawdz czy adres ip tego serwera jest już w tabeli a jeśli nie to go dodaj
                ResultSet wynik = executeQuery(st1, "SELECT * FROM serwery WHERE IP='"+ ip+"';");
                if (!wynik.next())
                {
                    executeUpdate(st1, "INSERT INTO serwery (IP ) VALUES ('"+ ip+"'); ");
                }
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Serwery (IP VARCHAR(50) unique NOT NULL);") > -1)
                {
                    System.out.println("Tabela Serwery utworzona");
                    executeUpdate(st1, "INSERT INTO serwery (IP ) VALUES ('"+ ip+"'); ");
                }
                else
                {
                    System.out.println("Tabela Serwery nie utworzona!");
                }
            }
            tables = dbm.getTables(null, null, "Zmiany", null);
            if (tables.next()) {
                System.out.println("Tabela Zmiany istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Zmiany (Tabela VARCHAR(50) NOT NULL, Klucz VARCHAR(50) NOT NULL, KolumnaDoZmiany VARCHAR(50) NOT NULL, NowaWartosc VARCHAR(150) NOT NULL);") > -1)
                    System.out.println("Tabela Zmiany utworzona");
                else
                    System.out.println("Tabela Zmiany nie utworzona!");
            }
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return connection;
    }

    /**
     * tworzenie obiektu Statement przesyłającego zapytania do bazy connection
     *
     * @param connection
     *            - połączenie z bazą
     * @return obiekt Statement przesyłający zapytania do bazy
     */
    private static Statement createStatement(Connection connection) {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ;
        return null;
    }

    /**
     * Wykonanie kwerendy i przesłanie wyników do obiektu ResultSet
     *
     * @param s
     *            - Statement
     * @param sql
     *            - zapytanie
     * @return wynik
     */
    private static ResultSet executeQuery(Statement s, String sql) {
        try {
            return s.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int executeUpdate(Statement s, String sql) {
        try {
            return s.executeUpdate(sql);
        }
        catch (MySQLIntegrityConstraintViolationException sqle)
        {
            return -5; //5 - dodawanie loginu, lub email który już jest w tabeli
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public void run()
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(csocket.getOutputStream()));
            Menu(in,out);
        }
        catch (IOException ex)
        {
            System.out.println("IOException");
        }
    }

    private void Menu (BufferedReader in, PrintWriter out)
    {
        try
        {
            /**
             * na podstawie pierwszej wyslanej lini serwer decyduje co ma zrobic
             */
            String tekst=in.readLine();
            if(tekst.equals("rejestracja"))
            {
                rejestracja(in,out);
            }
            if(tekst.equals("logowanie"))
            {
                logowanie(in,out);
            }
            if(tekst.equals("zmiana_danych"))
            {
                System.out.println("ładowanie danych");
                wyslijDaneDoZmiany(in, out);
            }
            if(tekst.equals("przypomnienie"))
            {
                System.out.println("przypomnienie hasła");
                przypomnienie(in,out);
            }
            if(tekst.equals("zmiana"))
            {
                System.out.println("zmiana danych");
                zmianaDanych(in,out);
            }
            if(tekst.equals("lista_prowadzacych"))
            {
                System.out.println("Wysyłanie listy wykładowców");
                wyslijListeWykladowcow(in,out);
            }
            if(tekst.equals("nowy_przedmiot"))
            {
                System.out.println("Tworzenie nowego przedmiotu");
                nowyPrzedmiot(in,out);
            }
            if(tekst.equals("lista_zajec"))
            {
                System.out.println("Wysyłanie listy zajęć");
                wyslijListeZajec(in,out);
            }
            if(tekst.equals("lista zajec na ktore nie jest zapisany"))
            {
                System.out.println("Wysyłanie listy zajęć");
                wyslijListeZajecNaKtoreNieJestZapisany(in,out);
            }
            if(tekst.equals("lista nieprowadzonych zajec"))
            {
                System.out.println("Wysyłanie listy nieprowadzonych zajęć");
                wyslijListeNieprowadzonychZajec(in,out);
            }
            if(tekst.equals("lista prowadzonych zajec"))
            {
                System.out.println("Wysyłanie listy prowadzonych zajęć");
                wyslijListeProwadzonychZajec(in,out);
            }
            if(tekst.equals("lista niezatwierdzonych"))
            {
                System.out.println("Wysyłanie listy niezatwierdzonych");
                wyslijListeNiezatwierdzonych(in,out);
            }
            if(tekst.equals("zapisz na zajecia"))
            {
                System.out.println("Zapisywanie na zajęcia");
                zapiszNaZajecia(in,out);
            }
            if(tekst.equals("prowadz nowe zajecia"))
            {
                System.out.println("Prowadzenie nowych zajęć");
                prowadzNoweZajecia(in,out);
            }
            if(tekst.equals("zatwierdzanie uzytkownika"))
            {
                System.out.println("Zatwierdzanie użytkownika");
                zatwierdzUzytkownika(in,out);
            }
            if(tekst.equals("zmiana preferowanych godzin"))
            {
                System.out.println("Zmiana preferowanych godzin");
                zmianaPreferowanychGodzin(in,out);
            }
            if(tekst.equals("wylogowanie"))
            {
                System.out.println("wylogowano");
                TypZalogowanego="";
                ObecnieZalogowany="";
                Menu(in,out);
            }
            if(tekst.equals("login szczegoly"))
            {
                System.out.println("Wysyłanie szczegółów konta");
                loginSzczegoly(in,out);
            }
            if(tekst.equals("lista zmian"))
            {
                System.out.println("Wysyłanie listy zmian");
                wyslijListeZmian(in,out);
            }
            if(tekst.equals("zatwierdzanie zmian"))
            {
                System.out.println("Zatwierdzanie zmian");
                zatwierdzZmiane(in,out);
            }
            if(tekst.equals("usuwanie zmian"))
            {
                System.out.println("usuwanie zmian");
                usunZmiane(in,out);
            }
            if(tekst.equals("usuwanie uzytkownika"))
            {
                System.out.println("usuwanie uzytkownika");
                usunUzytkownika(in,out);
            }
            if(tekst.equals("lista uzytkownikow"))
            {
                System.out.println("wysyłanie listy użytkowników");
                wyslijListeUzytkownikow(in,out);
            }
            if(tekst.equals("lista zajec ze szczegolowymi informacjami"))
            {
                System.out.println("wysyłanie szczegółowej listy zajęć");
                wyslijSzczegolowaListeZajec(in,out);
            }
            if(tekst.equals("usuwanie zajec"))
            {
                System.out.println("usuwanie przedmiotu");
                usunPrzedmiot(in,out);
            }
            if(tekst.equals("plan lekcji"))
            {
                System.out.println("wysyłanie planu lekcji");
                wyslijPlanLekcji(in,out);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception
    {
        if (ladujSterownik())
            System.out.println(" sterownik OK");
        else
            System.exit(1);
        Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
        ServerSocket ssock = new ServerSocket(4355);
        while (true)
        {
            Socket sock = ssock.accept();
            new Thread(new Serwer(sock)).start();
        }
    }

    private void logowanie(BufferedReader in, PrintWriter out)
    {
        String login,hasło;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE haslo='"+hasło+"' and login='"+login+"';");
            if (wyniklogowania.next())
            {
                boolean zatwierdzony =wyniklogowania.getBoolean("CzyZatwierdzony");
                String typ= wyniklogowania.getString("typ");
                if (zatwierdzony)
                {
                    System.out.println("Udalo sie zalogowac");
                    out.println("poprawne");
                    out.println(typ);
                    TypZalogowanego=typ;
                    ObecnieZalogowany=login;
                }
                else
                {
                    System.out.println("Nie zatwierdzony");
                    out.println("nie zatwierdzony");
                }
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void rejestracja(BufferedReader in, PrintWriter out)
    {
        String login,hasło,Imie,Nazwisko,Email,typ;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Imie = in.readLine();
            Nazwisko = in.readLine();
            Email = in.readLine();
            typ = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") > -1)
            {
                System.out.println("Zarejestrowano uzytkownika");
                out.println("ok");
            }
            else if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zarejestrowano uzytkownika!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }

    private void przypomnienie(BufferedReader in, PrintWriter out)
    {
        String email;
        try
        {
            email = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE Email='"+email+"';");
            if (wyniklogowania.next())
            {
                System.out.println("Znaleziono adres email w bazie");
                String login= wyniklogowania.getString("login");
                String haslo= wyniklogowania.getString("haslo");
                try
                {
                    GoogleMail.send(GoogleMail.getGmailService(),email,"","pwj.planlekcji@gmail.com","Pwj - plan lekcji. Przypomnienie hasła","login - "+login+"\nhasło - "+haslo );
                    out.println("poprawne");
                }
                catch (GoogleJsonResponseException e)
                {
                    out.println("zły email");
                }
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijDaneDoZmiany(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE login='"+ObecnieZalogowany+"';");
            if (wyniklogowania.next())
            {
                String haslo,imie,nazwisko,email;
                haslo= wyniklogowania.getString("haslo");
                imie= wyniklogowania.getString("Imie");
                nazwisko= wyniklogowania.getString("Nazwisko");
                email= wyniklogowania.getString("Email");
                out.println(ObecnieZalogowany);
                out.println(haslo);
                out.println(imie);
                out.println(nazwisko);
                out.println(email);
            }
            else
            {
                System.out.println("Brak zalogowanego uzytkownika w bazie");
                out.println("brak");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zmianaDanych(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login,hasło,imie,nazwisko,email;
            login=in.readLine();
            hasło=in.readLine();
            imie=in.readLine();
            nazwisko=in.readLine();
            email=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") > -1)
            {
                System.out.println("Zmieniono dane");
                ObecnieZalogowany=login;
                out.println("ok");
            }
            else if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zmieniono danych!");
                out.println("bledne");
            }
            out.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Menu(in,out);
    }

    private void wyslijListeWykladowcow(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE typ='prowadzacy';");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
            }
            out.println(size);
            while (wyniklogowania.next())
            {
                String nazwisko;
                nazwisko= wyniklogowania.getString("Nazwisko");
                out.println(nazwisko);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void nowyPrzedmiot(BufferedReader in, PrintWriter out)
    {
        String nazwa,Nazwisko;
        try
        {
            if (!TypZalogowanego.equals("administrator"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Nazwisko = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") > -1)
                {
                    System.out.println("Dodano przedmiot");
                    out.println("ok");
                }
                else if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") == -5)
                {
                    System.out.println("Duplikat nazwy");
                    out.println("duplikat");
                }
                else
                {
                    System.out.println("Nie dodano przedmiotu!");
                    out.println("bledne");
                }
            }
            out.flush();
            ustalGodzinyPrzedmiotow(in,out);
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` WHERE 1;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZajecNaKtoreNieJestZapisany(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            // przedmioty na które nie jest zapisany
            ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Uczeszczajacy NOT LIKE '% "+getNazwiskoZalogowanego()+",%' or Uczeszczajacy is null;");
            int size= 0;
            if (wynik != null)
            {
                wynik.beforeFirst();
                wynik.last();
                size = wynik.getRow();
                wynik.beforeFirst();
                out.println(size);
                while (wynik.next())
                {
                    String nazwa;
                    nazwa= wynik.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeNieprowadzonychZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` where Nazwisko_prowadzacego!='"+getNazwiskoZalogowanego()+"' ;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeProwadzonychZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` where Nazwisko_prowadzacego='"+getNazwiskoZalogowanego()+"' ;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zapiszNaZajecia(BufferedReader in, PrintWriter out)
    {
        String nazwa,nazwisko;
        try
        {
            nazwisko=getNazwiskoZalogowanego();
            if (!TypZalogowanego.equals("student"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Nazwa='"+nazwa+"'");
                try
                {
                    if (wynik.next())
                    {
                        String uczeszczajacy= wynik.getString("Uczeszczajacy");
                        if (uczeszczajacy==null)
                        {
                            uczeszczajacy="";
                        }
                        //jest już zapisany na przemiot
                        if (uczeszczajacy.contains(" "+nazwisko+","))
                        {
                            System.out.println("Jest już zapisany na przedmiot!");
                            out.println("duplikat2");
                        }
                        // nie jest zapisany na przedmiot
                        else
                        {
                            try
                            {
                                ResultSet duplikat = executeQuery(st, "SELECT * FROM `zmiany` WHERE Klucz='"+nazwa+"'and Tabela='przedmioty' and KolumnaDoZmiany='Uczeszczajacy' and NowaWartosc='"+nazwisko+"'");
                                // ta zmiana już jest w bazie danych
                                if (duplikat.next())
                                {
                                    System.out.println("Zmiana już jest w bazie danych");
                                    out.println("duplikat");
                                }
                                //dodawane do tabeli zmian w której będzie oczekiwało na zaakceptowanie przez administratora
                                else
                                {
                                    if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Uczeszczajacy', '"+nazwisko+"')") > -1)
                                    {
                                        System.out.println("Dodano do kolumny zmian");
                                        out.println("ok");
                                    }
                                    else
                                    {
                                        System.out.println("Nie dodano!");
                                        out.println("bledne");
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Nie znaleziono przedmiotu!");
                        out.println("bledne");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private String getNazwiskoZalogowanego ()
    {
        String nazwisko="";
        Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
        Statement st = createStatement(con);
        ResultSet wynik = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE login='"+ObecnieZalogowany+"'");
        try
        {
            if (wynik.next())
            {
                nazwisko= wynik.getString("Nazwisko");
            }
            else
            {
                System.out.println("Brak zalogowanego uzytkownika w bazie");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return nazwisko;
    }

    private void prowadzNoweZajecia(BufferedReader in, PrintWriter out)
    {
        String nazwa,nazwisko;
        try
        {
            nazwisko=getNazwiskoZalogowanego();
            if (!TypZalogowanego.equals("prowadzacy"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Nazwa='"+nazwa+"'");
                try
                {
                    if (wynik.next())
                    {
                        String nazwisko_prowadzacego= wynik.getString("Nazwisko_prowadzacego");
                        //już prowadzi przedmiot
                        if (nazwisko_prowadzacego.equals(nazwisko))
                        {
                            System.out.println("Już prowadzi przedmiot!");
                            out.println("duplikat2");
                        }
                        // nie prowadzi przedmiotu
                        else
                        {
                            try
                            {
                                ResultSet duplikat = executeQuery(st, "SELECT * FROM `zmiany` WHERE Klucz='"+nazwa+"'and Tabela='przedmioty' and KolumnaDoZmiany='Nazwisko_prowadzacego' and NowaWartosc='"+nazwisko+"'");
                                // ta zmiana już jest w bazie danych
                                if (duplikat.next())
                                {
                                    System.out.println("Zmiana już jest w bazie danych");
                                    out.println("duplikat");
                                }
                                //dodawane do tabeli zmian w której będzie oczekiwało na zaakceptowanie przez administratora
                                else
                                {
                                    if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Nazwisko_prowadzacego', '"+nazwisko+"')") > -1)
                                    {
                                        System.out.println("Dodano do kolumny zmian");
                                        out.println("ok");
                                    }
                                    else
                                    {
                                        System.out.println("Nie dodano!");
                                        out.println("bledne");
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Nie znaleziono przedmiotu!");
                        out.println("bledne");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void zmianaPreferowanychGodzin(BufferedReader in, PrintWriter out)
    {
        try
        {
            String nazwa=in.readLine();
            String godziny=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Preferowany_czas_prowadzacego', '"+godziny+"')") > -1)
            {
                System.out.println("Dodano do kolumny zmian");
                out.println("ok");
                out.flush();
            }
            else
            {
                System.out.println("Nie zmieniono!");
                out.println("bledne");
                out.flush();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Menu(in,out);
    }

    private void wyslijListeNiezatwierdzonych(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` where CzyZatwierdzony="+0);
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String login= wyniklogowania.getString("login");
                    String imie= wyniklogowania.getString("Imie");
                    String nazwisko= wyniklogowania.getString("Nazwisko");
                    String email= wyniklogowania.getString("Email");
                    String typ= wyniklogowania.getString("typ");
                    out.println(login);
                    out.println(imie);
                    out.println(nazwisko);
                    out.println(email);
                    out.println(typ);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zatwierdzUzytkownika(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "UPDATE `uzytkownicy` SET CzyZatwierdzony=1 where login='"+login+"'") > -1)
            {
                System.out.println("Zatwierdzono użytkownika");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie zatwierdzono użytkownika!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void loginSzczegoly(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE login='"+login+"';");
            if (wyniklogowania.next())
            {
                out.println("ok");
                String imie= wyniklogowania.getString("Imie");
                String nazwisko= wyniklogowania.getString("Nazwisko");
                String email= wyniklogowania.getString("Email");
                String typ= wyniklogowania.getString("typ");
                out.println(imie);
                out.println(nazwisko);
                out.println(email);
                out.println(typ);
            }
            else
            {
                System.out.println("Brak uzytkownika w bazie");
                out.println("brak");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZmian(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wynik = executeQuery(st, "SELECT * FROM `zmiany` WHERE 1;");
            int size= 0;
            if (wynik != null)
            {
                wynik.beforeFirst();
                wynik.last();
                size = wynik.getRow();
                wynik.beforeFirst();
                out.println(size);
                while (wynik.next())
                {
                    out.println(wynik.getString("Tabela"));
                    out.println(wynik.getString("Klucz"));
                    out.println(wynik.getString("KolumnaDoZmiany"));
                    out.println(wynik.getString("NowaWartosc"));
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zatwierdzZmiane(BufferedReader in, PrintWriter out)
    {
        try
        {
            String Tabela, Klucz, Kolumna, Wartosc, NazwaKlucz="";
            Tabela=in.readLine();
            Klucz=in.readLine();
            Kolumna=in.readLine();
            Wartosc=in.readLine();
            String nowaWartosc=Wartosc;
            switch (Tabela)
            {
                case "przedmioty" :
                    NazwaKlucz="Nazwa";
                    break;
                case "uzytkownicy" :
                    NazwaKlucz="login";
                    break;
            }
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);

            // dodawanie użytkowników zmienia, a nie zastępuje dotychczasową wartość w bazie danych
            if (Tabela.equals("przedmioty") && Kolumna.equals("Uczeszczajacy"))
            {
                ResultSet wynik = executeQuery(st, "Select Uczeszczajacy From przedmioty where "+NazwaKlucz+"='"+Klucz+"'");
                wynik.next();
                String lista= wynik.getString("Uczeszczajacy");
                if (lista==null)
                    lista="";
                nowaWartosc=lista+Wartosc+",";
            }
            if (executeUpdate(st, "UPDATE `"+Tabela+"` SET "+Kolumna+"='"+nowaWartosc+"' where "+NazwaKlucz+"='"+Klucz+"'") > -1)
            {
                System.out.println("Wykonano zmiane");
                if (executeUpdate(st, "DELETE FROM `zmiany` WHERE NowaWartosc ='"+Wartosc+"' and Tabela ='"+Tabela+"' and KolumnaDoZmiany ='"+Kolumna+"' and Klucz ='"+Klucz+"' ") > -1)
                {
                    System.out.println("Usunieto zmiane");
                    out.println("ok");
                }
                else
                {
                    System.out.println("Nie usunieto zmiany!");
                    out.println("nie usunieto zmiany");
                }
                // przy zmianie preferowanych godzin plan lekcji jest odświeżany
                if (Tabela.equals("przedmioty") && Kolumna.equals("Preferowany_czas_prowadzacego"))
                {
                    ustalGodzinyPrzedmiotow(in,out);
                }
                // przy zmianie prowadzącego zajęć plan lekcji jest odświeżany
                if (Tabela.equals("przedmioty") && Kolumna.equals("Nazwisko_prowadzacego"))
                {
                    ustalGodzinyPrzedmiotow(in,out);
                }
            }
            else
            {
                System.out.println("Nie wykonano zmiany!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void usunZmiane(BufferedReader in, PrintWriter out)
    {
        try
        {
            String Tabela, Klucz, Kolumna, Wartosc;
            Tabela=in.readLine();
            Klucz=in.readLine();
            Kolumna=in.readLine();
            Wartosc=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "DELETE FROM `zmiany` WHERE NowaWartosc ='"+Wartosc+"' and Tabela ='"+Tabela+"' and KolumnaDoZmiany ='"+Kolumna+"' and Klucz ='"+Klucz+"' ") > -1)
            {
                System.out.println("Usunieto zmiane");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie usunieto zmiany!");
                out.println("nie usunieto zmiany");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void usunUzytkownika(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login;
            login=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "DELETE FROM `uzytkownicy` WHERE login ='"+login+"'") > -1)
            {
                System.out.println("Usunieto uzytkownika");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie usunieto uzytkownika!");
                out.println("nie usunieto zmiany");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeUzytkownikow(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` where login!='"+ObecnieZalogowany+"'");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String login= wyniklogowania.getString("login");
                    String imie= wyniklogowania.getString("Imie");
                    String nazwisko= wyniklogowania.getString("Nazwisko");
                    String email= wyniklogowania.getString("Email");
                    String typ= wyniklogowania.getString("typ");
                    String czyZatwierdzono=wyniklogowania.getString("CzyZatwierdzony");
                    out.println(login);
                    out.println(imie);
                    out.println(nazwisko);
                    out.println(email);
                    out.println(typ);
                    out.println(czyZatwierdzono);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijSzczegolowaListeZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` WHERE 1;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa= wyniklogowania.getString("Nazwa");
                    String nazwisko= wyniklogowania.getString("Nazwisko_prowadzacego");
                    String godziny= wyniklogowania.getString("Godziny_przedmiotu");
                    int ileStudentow=0;
                    String studentci = wyniklogowania.getString("Uczeszczajacy");
                    if (studentci==null)
                        studentci="";
                    // jest tylu uczęszczających na przedmiot ile jest przecinków
                    for(int i = 0; i < studentci.length(); i++) {
                        if(studentci.charAt(i) == ',') ileStudentow++;
                    }
                    out.println(nazwa);
                    out.println(nazwisko);
                    out.println(godziny);
                    out.println(ileStudentow);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void usunPrzedmiot(BufferedReader in, PrintWriter out)
    {
        try
        {
            String nazwa;
            nazwa=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "DELETE FROM `przedmioty` WHERE Nazwa ='"+nazwa+"'") > -1)
            {
                System.out.println("Usunieto przedmiot");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie usunięto przedmiotu!");
                out.println("nie usunieto");
            }
            out.flush();
            ustalGodzinyPrzedmiotow(in,out);
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void ustalGodzinyPrzedmiotow (BufferedReader in, PrintWriter out)
    {
        System.out.println("ustalanie godzin przedmiotu");
        int ileSal= 1;
        Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
        Statement st = createStatement(con);
        /**
         * Tabela w która oznacza zajęte godziny [dzień tygodnia][godzina][sala].
         * Jeśli pole jest nullem to jest wolne, przy zajęciu wpisywane jest nazwisko prowadzącego
         * pierwszy wymiar to dzień tygodnia w którym będzie odbywał się przedmiot (0 - poniedziałek, 1 - wtorek itd.)
         * drugi wymiar to godzina zajęć, zajęcia odbywają się w takich godzinach jak na politechnice krakowskiej (0 - 7:30, 1 - 9:15, 2 - 11:00 itd.)
         * trzeci wymiar to sala w której będzie odbywał się przedmiot
         */
        String [][][] godzinyPrzedmiotow = new String[7][8][ileSal];
        ArrayList<String> listaOsbobDoEmail = new ArrayList<String>();
        ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` ORDER BY Preferowany_czas_prowadzacego DESC;");
        try
        {
            while (wynik.next())
            {
                boolean znalezionoCzas = false;
                String preferowaneGodziny= wynik.getString("Preferowany_czas_prowadzacego");
                String nazwisko= wynik.getString("Nazwisko_prowadzacego");
                String dotychczasowaGodzina= wynik.getString("Godziny_przedmiotu");
                String studenci = wynik.getString("Uczeszczajacy");
                int ileOkresow=0;

                //są preferowane okresy
                if (preferowaneGodziny!=null)
                {
                    String [] preferowaneOkresy=preferowaneGodziny.split(", ");
                    //sprawdzaj po kolei preferowane okresy
                    for (int i=0; i<preferowaneOkresy.length;i++)
                    {
                        String [] aktualny = preferowaneOkresy[i].split(" ");
                        int dzienTygodnia=0;
                        switch (aktualny[0])
                        {
                            case "Poniedzialek":
                                dzienTygodnia=0;
                                break;
                            case "Wtorek":
                                dzienTygodnia=1;
                                break;
                            case "Sroda":
                                dzienTygodnia=2;
                                break;
                            case "Czwartek":
                                dzienTygodnia=3;
                                break;
                            case "Piatek":
                                dzienTygodnia=4;
                                break;
                            case "Sobota":
                                dzienTygodnia=5;
                                break;
                            case "Niedziela":
                                dzienTygodnia=6;
                                break;
                        }
                        String [] godziny=aktualny[1].split("-");
                        int poczatek=Integer.parseInt(godziny[0]);
                        int koniec=Integer.parseInt(godziny[1]);
                        //indeksy 1 i ostatniej godziny które pasują do preferowanych godzin
                        int pierwszaGodzina=0, ostatniaGodzina=0;
                        for (int j=0; j<8;j++)
                        {
                            if (j*105+7*60+30>=poczatek)
                            {
                                pierwszaGodzina=j;
                                break;
                            }
                        }
                        for (int j=7; j>-1;j--)
                        {
                            if (j*105+7*60+30<=koniec)
                            {
                                ostatniaGodzina=j;
                                break;
                            }
                        }
                        // sprawdź po kolei pasująće godziny
                        for (int j=pierwszaGodzina;j<ostatniaGodzina;j++)
                        {
                            boolean juzMaZajeciaOTejGodzinie=false;
                            // sprawdź po kolei sale
                            for (int k=0;k<godzinyPrzedmiotow[0][0].length;k++)
                            {
                                if (godzinyPrzedmiotow[dzienTygodnia][j][k]!=null)
                                {
                                    if (godzinyPrzedmiotow[dzienTygodnia][j][k].equals(nazwisko))
                                        juzMaZajeciaOTejGodzinie=true;
                                }
                                //jeśli godzina jest wolna i nie ma zajęć w tej godzinie
                                if (godzinyPrzedmiotow[dzienTygodnia][j][k]==null && !juzMaZajeciaOTejGodzinie)
                                {
                                    godzinyPrzedmiotow[dzienTygodnia][j][k]=nazwisko;
                                    znalezionoCzas=true;

                                    String Nazwa= wynik.getString("Nazwa");
                                    String godzina=aktualny[0];
                                    int minuty=7*60+30+(j*105);
                                    godzina=godzina+" "+minuty+"-"+(minuty+90);
                                    Statement st1 = createStatement(con);
                                    executeUpdate(st1, "UPDATE przedmioty SET Godziny_przedmiotu='"+godzina+"' WHERE Nazwa='"+Nazwa+"'");
                                    //jeśli dotychczasowa godzina przedmiotu była inna to dodaj prowadzącego i studentów do listy osób któa otrzyma maila z informacją o zmianie planu
                                    if (dotychczasowaGodzina==null || !dotychczasowaGodzina.equals(godzina))
                                    {
                                        if (studenci!=null)
                                        {
                                            String [] tablicaStudentow=studenci.split(",");
                                            for (int t=0;t<tablicaStudentow.length;t++)
                                            {
                                                boolean znaleziono=false;
                                                for (int t1=0;t1<listaOsbobDoEmail.size();t1++)
                                                {
                                                    //ustaw flage na true jeśli znaleziono studenta w liście osób
                                                    if (tablicaStudentow[t].equals(listaOsbobDoEmail.get(t1)))
                                                    {
                                                        znaleziono=true;
                                                    }
                                                    if (znaleziono)
                                                        break;
                                                }
                                                if (!znaleziono)
                                                    listaOsbobDoEmail.add(tablicaStudentow[t]);
                                            }
                                            // szukanie prowadzącego na liście do email
                                            boolean znaleziono=false;
                                            for (int t1=0;t1<listaOsbobDoEmail.size();t1++)
                                            {
                                                //ustaw flage na true jeśli znaleziono prowadzącego w liście osób
                                                if (nazwisko.equals(listaOsbobDoEmail.get(t1)))
                                                {
                                                    znaleziono=true;
                                                }
                                                if (znaleziono)
                                                    break;
                                            }
                                            if (!znaleziono)
                                                listaOsbobDoEmail.add(nazwisko);
                                        }
                                    }
                                }
                                if (znalezionoCzas)
                                    break;
                            }
                            if (znalezionoCzas)
                                break;
                        }
                        if (znalezionoCzas)
                            break;
                    }
                }
                //nie znaleziono jeszcze czasu, czyli albo nie ma preferowanych okresów, albo są już zajęte
                if (!znalezionoCzas)
                {
                    // sprawdź po kolei dni
                    for (int i=0; i< godzinyPrzedmiotow.length; i++)
                    {
                        // sprawdź po kolei godziny
                        for (int j=0;j<godzinyPrzedmiotow[0].length;j++)
                        {
                            boolean juzMaZajeciaOTejGodzinie=false;
                            // sprawdź po kolei sale
                            for (int k=0;k<godzinyPrzedmiotow[0][0].length;k++)
                            {
                                if (godzinyPrzedmiotow[i][j][k]!=null)
                                {
                                    if (godzinyPrzedmiotow[i][j][k].equals(nazwisko))
                                        juzMaZajeciaOTejGodzinie=true;
                                }
                                //jeśli godzina jest wolna i nie ma zajęć w tej godzinie
                                if (godzinyPrzedmiotow[i][j][k]==null && !juzMaZajeciaOTejGodzinie)
                                {
                                    godzinyPrzedmiotow[i][j][k]=nazwisko;
                                    znalezionoCzas=true;

                                    String Nazwa= wynik.getString("Nazwa");
                                    String godzina="";
                                    switch (i)
                                    {
                                        case 0:
                                            godzina="Poniedzialek";
                                            break;
                                        case 1:
                                            godzina="Wtorek";
                                            break;
                                        case 2:
                                            godzina="Sroda";
                                            break;
                                        case 3:
                                            godzina="Czwartek";
                                            break;
                                        case 4:
                                            godzina="Piatek";
                                            break;
                                        case 5:
                                            godzina="Sobota";
                                            break;
                                        case 6:
                                            godzina="Niedziela";
                                            break;
                                    }
                                    int minuty=7*60+30+(j*105);
                                    godzina=godzina+" "+minuty+"-"+(minuty+90);
                                    Statement st1 = createStatement(con);
                                    executeUpdate(st1, "UPDATE przedmioty SET Godziny_przedmiotu='"+godzina+"' WHERE Nazwa='"+Nazwa+"'");
                                    //jeśli dotychczasowa godzina przedmiotu była inna to dodaj prowadzącego i studentów do listy osób któa otrzyma maila z informacją o zmianie planu
                                    if (dotychczasowaGodzina==null || !dotychczasowaGodzina.equals(godzina))
                                    {
                                        if (studenci!=null)
                                        {
                                            String [] tablicaStudentow=studenci.split(",");
                                            for (int t=0;t<tablicaStudentow.length;t++)
                                            {
                                                boolean znaleziono=false;
                                                for (int t1=0;t1<listaOsbobDoEmail.size();t1++)
                                                {
                                                    //ustaw flage na true jeśli znaleziono studenta w liście osób
                                                    if (tablicaStudentow[t].equals(listaOsbobDoEmail.get(t1)))
                                                    {
                                                        znaleziono=true;
                                                    }
                                                    if (znaleziono)
                                                        break;
                                                }
                                                if (!znaleziono)
                                                    listaOsbobDoEmail.add(tablicaStudentow[t]);
                                            }
                                            // szukanie prowadzącego na liście do email
                                            boolean znaleziono=false;
                                            for (int t1=0;t1<listaOsbobDoEmail.size();t1++)
                                            {
                                                //ustaw flage na true jeśli znaleziono prowadzącego w liście osób
                                                if (nazwisko.equals(listaOsbobDoEmail.get(t1)))
                                                {
                                                    znaleziono=true;
                                                }
                                                if (znaleziono)
                                                    break;
                                            }
                                            if (!znaleziono)
                                                listaOsbobDoEmail.add(nazwisko);
                                        }
                                    }
                                }
                                if (znalezionoCzas)
                                    break;
                            }
                            if (znalezionoCzas)
                                break;
                        }
                        if (znalezionoCzas)
                            break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            emailZInformacjaOZmianiePlanu(listaOsbobDoEmail);
        }
    }

    private void wyslijPlanLekcji (BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            Statement st1 = createStatement(con);
            ResultSet wyniklogin, wynik=null;
            wyniklogin = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE login='"+ObecnieZalogowany+"';");
            String nazwisko="";
            if (wyniklogin.next())
                nazwisko=wyniklogin.getString("Nazwisko");
            if (TypZalogowanego.equals("student"))
            {
                wynik=executeQuery(st1, "SELECT * FROM `przedmioty` WHERE Uczeszczajacy LIKE '%"+getNazwiskoZalogowanego()+",%';");
            }
            else if (TypZalogowanego.equals("prowadzacy"))
            {
                wynik=executeQuery(st1, "SELECT * FROM `przedmioty` WHERE Nazwisko_prowadzacego='"+nazwisko+"';");
            }
            int size= 0;
            if (wynik != null)
            {
                wynik.beforeFirst();
                wynik.last();
                size = wynik.getRow();
                wynik.beforeFirst();
                out.println(size);
                while (wynik.next())
                {
                    String nazwa= wynik.getString("Nazwa");
                    String godziny= wynik.getString("Godziny_przedmiotu");
                    String pole = "";
                    String [] bufor=godziny.split(" ");
                    switch (bufor[0])
                    {
                        case "Poniedzialek":
                            pole="Pon";
                            break;
                        case "Wtorek":
                            pole="Wt";
                            break;
                        case "Sroda":
                            pole="Sr";
                            break;
                        case "Czwartek":
                            pole="Czw";
                            break;
                        case "Piatek":
                            pole="Pia";
                            break;
                        case "Sobota":
                            pole="So";
                            break;
                        case "Niedziela":
                            pole="Nie";
                            break;
                    }

                    String [] minuty= bufor[1].split("-");
                    int minutyPoczatkowe=Integer.parseInt(minuty[0]);

                    switch (minutyPoczatkowe)
                    {
                        case 450:
                            pole=pole+"730";
                            break;
                        case 555:
                            pole=pole+"915";
                            break;
                        case 660:
                            pole=pole+"11";
                            break;
                        case 765:
                            pole=pole+"1245";
                            break;
                        case 870:
                            pole=pole+"1430";
                            break;
                        case 975:
                            pole=pole+"1615";
                            break;
                        case 1080:
                            pole=pole+"18";
                            break;
                        case 1185:
                            pole=pole+"1945";
                            break;
                    }
                    out.println(nazwa);
                    out.println(pole);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void emailZInformacjaOZmianiePlanu (ArrayList <String> listaOsob)
    {
        for (int i=0;i<listaOsob.size();i++)
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            ResultSet wynik = executeQuery(st, "SELECT * FROM `uzytkownicy` Where Nazwisko='"+listaOsob.get(i)+"';");
            try
            {
                while (wynik.next()) {
                    String email = wynik.getString("Email");
                    GoogleMail.send(GoogleMail.getGmailService(),email,"","pwj.planlekcji@gmail.com","Pwj - plan lekcji. Zmiana planu lekcji","Twój plan lekcji został zmieniony" );
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

}
