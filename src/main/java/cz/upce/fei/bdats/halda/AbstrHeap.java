package cz.upce.fei.bdats.halda;

// <editor-fold defaultstate="collapsed" desc="Importy">
import cz.upce.fei.bdats.sekvence.fifo.AbstrFifo;
import cz.upce.fei.bdats.sekvence.fifo.IAbstrFifo;
import cz.upce.fei.bdats.sekvence.lifo.AbstrLifo;
import cz.upce.fei.bdats.sekvence.lifo.IAbstrLifo;
import cz.upce.fei.bdats.strom.ETypProhl;
import cz.upce.fei.bdats.vyjimky.FifoException;
import cz.upce.fei.bdats.vyjimky.LifoException;
import cz.upce.fei.bdats.vyjimky.zpravy.FifoZprava;
import cz.upce.fei.bdats.vyjimky.zpravy.HeapZprava;
import cz.upce.fei.bdats.vyjimky.HeapException;
import cz.upce.fei.bdats.vyjimky.zpravy.LifoZprava;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
// </editor-fold>

/**
 * Třída implementuje sadu základních operací abstraktní datové strultury <i>(ADS)</i> <b>prioritní fronta</b>
 * rozšiřující ADS <b>binární vyhledávací strom</b> <i>(BVS - eng. Binary Search Tree)</i>
 *
 * <p> Třída implementuje <b>binární maximální levostrannou haldu</b> <i>(eng. Max Heap)</i>, což je binárním stromem,
 * kde hodnota každého předka je větší nebo rovna hodnotám jeho potomků <i>(levý a pravý syny)</i>, což zajišťuje
 * rychlý přístup k největšímu prvku v množině
 *
 * <p> Implementuje rozhraní {@link IAbstrHeap}
 *
 * @see java.util.PriorityQueue
 *
 * @param <E> Generický parametr reprezentující budoucí datový typ prvků prioritní fronty
 */
public final class AbstrHeap<E> implements IAbstrHeap<E> {

// <editor-fold defaultstate="collapsed" desc="Atributy/Instanční proměnné">
    /**
     * Uchvávač typu prvků prioritní fronty pro její vytvoření pomocí {@link Array#newInstance(Class, int)}
     */
    private final Class<E> typPrvku;
    /**
     * Kritérium porovnávání prvků (tj. priorita)
     */
    private Comparator<E> komparator;
    /**
     * Pole pro ukládání prvků haldy
     */
    private E[] halda;
    /**
     * Maximální možná velikost haldy
     */
    private int kapacita;
    /**
     * Aktuální velikost haldy (tj. počet prvků)
     */
    private int mohutnost;
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Konstruktory">
    /**
     * Konstanta reprezentuje výchozí inicializační hodnotu kapacity haldy
     */
    private static final int VYCHOZI_KAPACITA = 11;

    public AbstrHeap(Class<E> typPrvku,
                     Comparator<E> komp,
                     int kapacita) throws HeapException {
        pozadatPlatnouKapacitu(kapacita);
        pozadatNePrazdnyKomparator(komp);

        this.typPrvku = typPrvku;
        this.komparator = komp;
        this.halda = (E[]) Array.newInstance(typPrvku, kapacita);
        this.kapacita = kapacita;
        this.mohutnost = 0;
    }

    public AbstrHeap(Class<E> typPrvku,
                     Comparator<E> komp) throws HeapException {
        this(typPrvku, komp, VYCHOZI_KAPACITA);
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: void vybuduj(E[] pole)">
    @Override
    public void vybuduj(E[] pole) throws HeapException {
        pozadatNeprazdnePole(pole);

        this.kapacita = pole.length;
        this.mohutnost = dejPocetPrvku(pole);
        this.halda = (E[]) Array.newInstance(typPrvku, pole.length);

        System.arraycopy(pole, 0, halda, 0, pole.length);

        final int pulka = pole.length / 2;
        for (int i = pulka - 1; i >= 0; i--) {
            probublejPulku(i, pulka);
        }
    }

    /**
     * Vrátí počet ne {@code null} prvků v daném poli
     *
     * <p> <b>Poznámka</b>: Tuto metodu by šlo implementovat jedním řádkem:
     * <ul>
     * <li> <b>return (int) Arrays.stream(pole).filter(Objects::nonNull).count();</b>
     * </ul>
     *
     * @param pole Pole prvků
     *
     * @return Celkový počet obsazených míst v daném poli
     */
    private int dejPocetPrvku(E @NotNull [] pole) {
        int pocet = 0;
        for (E prvek: pole) {
            if (prvek != null)
                pocet++;
        }
        return pocet;
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: void reorganizuj(Comparator<E> comp)">
    @Override
    public void reorganizuj(Comparator<E> komp) throws HeapException {
        pozadatNePrazdnyKomparator(komp);

        this.komparator = komp;
        vybuduj(halda);
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: void zrus()">
    @Override
    public void zrus() {
        halda = (E[]) Array.newInstance(typPrvku, kapacita);
        mohutnost = 0;
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: boolean jePrazdna()">
    @Override
    public boolean jePrazdna() { return mohutnost == 0; }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: int mohutnost()">
    @Override
    public int mohutnost() { return mohutnost; }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: Iterator<E> vytvorIterator(ETypProhl typ)">
    @Override
    public Iterator<E> vytvorIterator(ETypProhl typ) {
        return switch (typ) {
            case SIRKA -> new PFSirkaIterator();
            case HLOUBKA -> new PFHloubkaIterator();
        };
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: void vloz(E prvek)">
    /**
     * Vloží prvek do haldy a zajistí udržení haldového uspořádaní
     *
     * <p> Popis jednotlivých bloků kódu:
     * <ol>
     * <li> <b>if(mohutnost == kapacita)</b>: Pokud je aktuální mohutnost větší než kapacita:
     *      <ul>
     *      <li> <b>zvysKapacitu()</b>: Zvětší kapacitu haldy
     *      </ul>
     * <li> Inkrementuje mohutnost a vloží prvek na konec pole haldy
     *      <ul>
     *      <li> <b>mohutnost++</b>
     *      <li> <b>halda[mohutnost - 1] = prvek</b>
     *      </ul>
     * <li> <b>probublejNahoru(mohutnost - 1)</b>: Probublá nově vložený prvek nahoru, aby byla zachována
     * haldové uspořádaní
     * </ol>
     */
    @Override
    public void vloz(E prvek) throws HeapException {
        pozadatNeprazdnyPrvek(prvek);

        zabezpecKapacitu();
        halda[mohutnost] = prvek;
        mohutnost++;
        probublejNahoru();
    }

    /**
     * Zdvojnásobí velikost pole, pokud se rovná celkocé kapacitě - <i>(eng. grow(int minCapacity))</i>
     *
     * <p> Kopírování prvků <b>System.arraycopy(halda, 0, novaHalda, 0, mohutnost)</b>:
     * <ol>
     * <li> <b>src</b>: Zdrojové pole, z něhož budou kopírovány prvky
     * <li> <b>srcPos</b>: Počáteční pozice v zdrojovém poli, odkud začne kopírování
     * <li> <b>dest</b>: Cílové pole, do něhož budou kopírovány prvky
     * <li> <b>destPos</b>: Počáteční pozice v cílovém poli, kam začne kopírování
     * <li> <b>length</b>: Počet prvků, jež budou zkopírovány
     * </ol>
     */
    private void zabezpecKapacitu() {
        if (mohutnost == kapacita) {
            final int novaKapacita = halda.length * 2;
            final E[] novaHalda = (E[]) Array.newInstance(typPrvku, novaKapacita);
            System.arraycopy(halda, 0, novaHalda, 0, mohutnost);
            halda = novaHalda;
            kapacita = novaKapacita;
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: E odeberMax()">
    /**
     * Odebere a vrátí prvek s maximální hodnotou z haldy a zajistí udržení haldového uspořádaní
     *
     * <p> Popis jednotlivých bloků kódu:
     * <ol>
     * <li> <b>final E maxPrvek = halda[0]</b>: Uloží prvek s nejvyšší prioritou (je vždy na začátku pole)
     * <li> Nahrazuje první prvek posledním a sníží mohutnost:
     *      <ul>
     *      <li> <b>halda[0] = halda[mohutnost - 1]</b>
     *      <li> <b>mohutnost--</b>
     *      </ul>
     * <li> <b>probublejDolu(0)</b>: Probublá dolu <i>(eng. heapifyDown)</i> na pozici, kde byl odebrán prvek
     * </ol>
     */
    @Override
    public E odeberMax() throws HeapException {
        pozadatNePrazdnouHaldu();

        final E maxPrvek = halda[0];
        halda[0] = halda[--mohutnost];
        probublejDolu();

        return maxPrvek;
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: E zpristupniMax()">
    @Override
    public E zpristupniMax() throws HeapException {
        pozadatNePrazdnouHaldu();
        return halda[0];
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Metoda: String vypis()">
    @Override
    public @NotNull String vypis(ETypProhl typ) throws HeapException {
        pozadatNeprazdnyTyp(typ);

        final StringBuilder sb = new StringBuilder();
        final Iterator<E> iterator = vytvorIterator(typ);
        while (iterator.hasNext()) {
            final E prvek = iterator.next();
            sb.append(prvek).append("\n");
        }
        sb.replace(sb.length() - 1,
                sb.length(),
                "");
        return sb.toString();
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Soukromá třída (BVS): Iterátor do šířky (BFS)">
    /**
     * Strategie <b>Breadth-First Traversal</b>/<b>Level-Order Traversal</b> prochází stromové/hierarchické struktury
     * postupně na jedné úrovni před tím, než se přesune na další úroveň
     *
     * <p> V kontextu <u>binární haldy</u> implementované na poli to znamená, že začne u kořene (index 0) a postupně
     * prochází všechny jeho potomky, až se dostane na další úroveň
     *
     * <p> Používá pomocnou datovou strukturu <u>fronta</u> <i>(eng. Queue)</i> uchovávající indexy prvků, jež zatím
     * nebyly zpracovány
     *
     * <p> <b>Poznámka</b>: Prihlídka je založena na stejném principu, jako u ADS <b>binární vyhledávací strom</b>
     */
    private class BVSSirkaIterator implements Iterator<E> {

        /**
         * Instanční proměnná uchovává indexy prvků haldy
         */
        private final IAbstrFifo<Integer> frontaIndexu;

        /**
         * Konstruktor inicializuje frontu a vloží index kořene {@code (0)} na konec fronty
         *
         * <p> <b>Poznámka</b>: Nezpracovává výjimku <b>catch (FifoException ignored) {}</b>, protože vstupní
         * data {@code (0)} nejsou nidky {@code null}
         */
        public BVSSirkaIterator() {
            this.frontaIndexu = new AbstrFifo<>();
            try {
                frontaIndexu.vlozNaKonec(0);
            } catch (FifoException ignored) {}
        }

        @Override
        public boolean hasNext() { return !frontaIndexu.jePrazdna(); }

        /**
         * Odebere prvek a vrátí z přední části fronty (korespondující s aktuálním prvkem) a získá index levého a
         * pravého syna tohoto prvku, jež vloží na konec fronty (pokud synové existují)
         *
         * @return Následující prvek v rámci iterace do šířky
         */
        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException(
                        FifoZprava.PRAZDNA_FRONTA.zprava());
            try {
                final int aktualniIndex = frontaIndexu.odeberZeZacatku();
                final E aktualniPrvek = halda[aktualniIndex];

                int indexLevehoSyna = indexLevehoSyna(aktualniIndex);
                if (indexLevehoSyna < mohutnost())
                    frontaIndexu.vlozNaKonec(indexLevehoSyna);

                final int indexPravehoSyna = indexPravehoSyna(aktualniIndex);
                if (indexPravehoSyna < mohutnost()) {
                    frontaIndexu.vlozNaKonec(indexPravehoSyna);
                }
                return aktualniPrvek;
            } catch (FifoException ex) {
                throw new NoSuchElementException(
                        FifoZprava.PRAZDNA_FRONTA.zprava());
            }
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Soukromá třída (BVS): Iterátor do hloubky, in-order (DFS)">
    /**
     * Taktika <b>in-order Depth-First Traversal</b> v kontextu <u>binární haldy</u>/<u>prioritní fronty</u>
     * (kde platí pravidlo, že každý uzel má nejvýše dva syny) znamená, že postupně prochází levým synem až do konce,
     * poté navštíví uzel a nakonec přejdeme na pravého syna (tzn. nejdříve navštíví levý podstrom, poté kořen a
     * nakonec pravý podstrom)
     *
     * <p> Pro implementaci strategie je využitá pomocná datová struktura <u>zásobník</u> <i>(eng. Stack)</i> zejména
     * jako výstupní soustava, tzn. udržuje aktuální stav průchod a následující kroky
     *
     * <p> <b>Poznámka</b>: Prohlídka je založena na stejném principu, jako u ADS <b>binární vyhledávací strom</b>
     */
    private class BVSHloubkaIterator implements Iterator<E> {

        /**
         * Instanční proměnná udržuje indexy prvků prioritní haldy
         */
        private final IAbstrLifo<Integer> zasobnikIndexu;

        /**
         * Konstruktor inicializuje zásobník a přidává kořenový uzel do zásobníku
         */
        public BVSHloubkaIterator() {
            this.zasobnikIndexu = new AbstrLifo<>();
            pridejDoZasobniku(0);
        }

        /**
         * Přidává uzly od daného indexu směrem vlevo do zásobníku
         *
         * <p> <b>Poznámka</b>: Nezpracovává výjimku <b>catch (LifoException ignored) {}</b>, protože vstupní
         * data {@code (index)} nejsou nidky {@code null}
         *
         * @param index Index, od něhož se začne postup
         *
         * @see AbstrHeap#indexLevehoSyna(int)
         */
        private void pridejDoZasobniku(int index) {
            try {
                while (index < mohutnost()) {
                    zasobnikIndexu.vlozNaZacatek(index);
                    index = indexLevehoSyna(index);
                }
            } catch (LifoException ignored) {}
        }

        @Override
        public boolean hasNext() {
            return !zasobnikIndexu.jePrazdny();
        }

        /**
         * Odebere index z vrcholu zásobníku a vrací příslušný prvek z haldy. Pak přidá do zásobníku pravého syna
         * odebraného uzlu (pokud existuje) a jeho levé syny
         *
         * @return Následující prvek v rámci průchodu do hloubky
         */
        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException(
                        LifoZprava.PRAZDNY_ZASOBNIK.zprava());
            try {
                final int aktualniIndex = zasobnikIndexu.odeberZeZacatku();
                final E aktualniPrvek = halda[aktualniIndex];

                final int pravyIndex = indexPravehoSyna(aktualniIndex);
                if (pravyIndex < mohutnost())
                    pridejDoZasobniku(pravyIndex);
                return aktualniPrvek;
            } catch (LifoException ex) {
                throw new NoSuchElementException(
                        LifoZprava.PRAZDNY_ZASOBNIK.zprava());
            }
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Privátní třída (Prioritní Fronta): Iterátor do šířky (BFS)">
    /**
     * @see BVSSirkaIterator
     */
    private class PFSirkaIterator implements Iterator<E> {

        private int aktualniIndex;

        public PFSirkaIterator() {
            this.aktualniIndex = 0;
        }

        @Override
        public boolean hasNext() { return aktualniIndex < mohutnost; }

        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException(
                        FifoZprava.PRAZDNA_FRONTA.zprava());
            return halda[aktualniIndex++];
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Privátní třída (Prioritní Fronta): Iterátor do hloubky, in-order (DFS)">
    /**
     * Strategie průchodu binárním stromem:
     * <ul>
     * <li> <b>In-order</b>: Nejprve se zpracovává levý podstrom, poté aktuální uzel a nakonec pravý podstrom - vrátí
     * posloupnost ve <i>vzestupně uspořádaném pořadí</i>
     * <li> <b>Pre-order</b>: Nejprve se zpracovává aktuální uzel, poté levý podstrom a nakonec pravý podstrom -
     * umožňuje rychlý přístup k vrcholům stromu
     * <li> <b>Post-order</b>: Nejprve se zpracovává levý podstrom, poté pravý podstrom a nakonec aktuální uzel -
     * zpracovává všechny potomky uzlu před samotným uzlem
     * </ul>
     *
     * <p> U této implementace dochází k používání principu <b>pre-order</b>. Jelikož <b>prioritní fronta</b> nemá tu
     * vlastnost, jež má ADT <b>tabulka</b>, tzn. nemá na abstraktní úrovni definovaný příznak <b>klíču</b>
     * představující unikátní identifikátor prvku, není zapotřebí implementovat prohlídku do hloubky typu <b>in-order</b>
     * umožňující vytvořit lineární uspořádání prvků podle hodnoty klíče, ale prohlídku typu <b>pre-order</b>
     * <i>(před-přehled)</i>, kde dochází k návštěvě nejprve kořenového uzlu, poté jeho levého podstromu a
     * nakonec pravého podstromu
     *
     * @see BVSHloubkaIterator
     */
    private class PFHloubkaIterator implements Iterator<E> {

        private final IAbstrLifo<Integer> zasobnikIndexu;

        /**
         * Konstruktor inicializuje zásobník a vloží do něho index kořene {@code (0)}
         */
        public PFHloubkaIterator() {
            this.zasobnikIndexu = new AbstrLifo<>();
            if (mohutnost > 0)
                zasobnikIndexu.vlozNaZacatek(0);
        }

        @Override
        public boolean hasNext() { return !zasobnikIndexu.jePrazdny(); }

        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException(
                        LifoZprava.PRAZDNY_ZASOBNIK.zprava());
            try {
                final int aktualniIndex = zasobnikIndexu.odeberZeZacatku();
                final E aktualniPrvek = halda[aktualniIndex];

                final int indexPravehoSyna = indexPravehoSyna(aktualniIndex);
                if (indexPravehoSyna < mohutnost)
                    zasobnikIndexu.vlozNaZacatek(indexPravehoSyna);

                final int indexLevehoSyna = indexLevehoSyna(aktualniIndex);
                if (indexLevehoSyna < mohutnost)
                    zasobnikIndexu.vlozNaZacatek(indexLevehoSyna);

                return aktualniPrvek;
            } catch (LifoException ex) {
                throw new NoSuchElementException(
                        LifoZprava.PRAZDNY_ZASOBNIK.zprava());
            }
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Privátní Metody: Dotazy typu pozadat()">
    /**
     * Zkontroluje, zda vstupní pole reprezentující haldu je prázdné
     *
     * @param pole Neuspořádaná halda
     *
     * @throws HeapException Když je pole prvků roveno {@code null}, anebo nemá žádné prvky
     *
     * @see AbstrHeap#vybuduj(Object[])
     */
    private void pozadatNeprazdnePole(E[] pole) throws HeapException {
        if (pole == null || pole.length == 0)
            throw new HeapException(
                    HeapZprava.PRAZDNE_POLE.zprava());
    }

    /**
     * Zkontroluje, zda vstupní prvek je {@code null}
     *
     * @param prvek Prvek pro vložení
     *
     * @throws HeapException Když je vstupní prvek {@code null}
     *
     * @see AbstrHeap#vloz(Object)
     */
    private void pozadatNeprazdnyPrvek(E prvek) throws HeapException {
        if (prvek == null)
            throw new HeapException(
                    HeapZprava.PRAZDNY_PRVEK.zprava());
    }

    /**
     * Zkontroluje, zda vstupní typ pro prohlížení haldy je {@code null}:
     * <ol>
     * <li> Do šířky <i>(eng. Breadth-First Search - BFS)</i>
     * <li> Do hloubky, <b>in-order</b> <i>(eng. Depth-First Search - DFS)</i>
     * </ol>
     *
     * @param typ Typ prohlížení
     *
     * @throws HeapException Když je vstupní typ {@code null}
     *
     * @see AbstrHeap#vypis(ETypProhl)
     */
    private void pozadatNeprazdnyTyp(ETypProhl typ) throws HeapException {
        if (typ == null)
            throw new HeapException(
                    HeapZprava.PRAZDNY_PRVEK.zprava());
    }

    /**
     * Zkontroluje, zda vstupní inicializační kapacita větší než {@code 0}
     *
     * @param kapacita Inicializační kapacita vstupující do konstruktoru třídy
     *
     * @throws HeapException Když je vstupní kapacita menší než {@code 1}
     *
     * @see AbstrHeap#AbstrHeap(Class, Comparator, int)
     */
    private void pozadatPlatnouKapacitu(int kapacita) throws HeapException {
        if (kapacita < 1)
            throw new HeapException(
                    HeapZprava.NULL_INICIALIZACNI_KAPACITA.zprava());
    }

    /**
     * Zkontroluje, zda vstupní komparátor je {@code null}
     *
     * @param komp Inicializační porovnájací kritérium (priorita), podle něhož se následně vybuduje halda
     *
     * @throws HeapException Když je vstupní komparátor {@code null}
     *
     * @see AbstrHeap#AbstrHeap(Class, Comparator, int)
     * @see AbstrHeap#reorganizuj(Comparator)
     */
    private void pozadatNePrazdnyKomparator(Comparator<E> komp) throws HeapException {
        if (komp == null)
            throw new HeapException(
                    HeapZprava.NULL_INICIALIZACNI_KOMPARATOR.zprava());
    }

    /**
     * Zkontroluje, zda prioritní fronta je prázdná
     *
     * @throws HeapException Když je halda prázdná
     *
     * @see AbstrHeap#odeberMax()
     * @see AbstrHeap#zpristupniMax()
     */
    private void pozadatNePrazdnouHaldu() throws HeapException {
        if (jePrazdna())
            throw new HeapException(
                    HeapZprava.PRAZDNA_HALDA.zprava());
    }

    /**
     * Zkontroluje, zda je alespoň jedno pole {@code null}
     *
     * @param src Zdrojové pole
     * @param dest Cílové pole
     *
     * @throws HeapException Když je alespoň jedno pole {@code null}
     *
     * @see AbstrHeap#zkopirujPole(Object[], Object[])
     */
    private void pozadatNePrazdnaPole(E[] src, E[] dest) throws HeapException {
        if (src == null || dest == null)
            throw new HeapException(
                    HeapZprava.KOPIROVANI_POLE_NULL.zprava());
    }

    /**
     * Zkontroluje, zda délky vstupních polí jsou stejné
     *
     * @param src Zdrojové pole
     * @param dest Cílové pole
     *
     * @throws HeapException Když délky obou polí nejsou stejné
     *
     * @see AbstrHeap#zkopirujPole(Object[], Object[])
     */
    private void pozadatStejnouDelkuPole(E @NotNull [] src, E @NotNull [] dest) throws HeapException {
        if (src.length != dest.length)
            throw new HeapException(
                    HeapZprava.KOPIROVANI_POLE_DELKA.zprava());
    }
// </editor-fold>

//<editor-fold defaultstate="collapsed" desc="Privátní Metody: Probublání prvků">
    /**
     * Probublá prvek (právě vložený na konec haldy) nahoru tak dlouho, dokud není splněno haldové uspořádání,
     * tzn. předek má větší nebo rovnou prioritu vůči svým potomkům <i>(eng. bubbleUp/heapifyUp/siftUp)</i>
     *
     * @see AbstrHeap#vloz(Object)
     */
    private void probublejNahoru() {
        int aktualniIndex = mohutnost - 1;
        while (jePredek(aktualniIndex) &&
                komparator.compare(predek(aktualniIndex),
                                   halda[aktualniIndex]) < 0) {
            vymen(indexPredka(aktualniIndex), aktualniIndex);
            aktualniIndex = indexPredka(aktualniIndex);
        }
    }

    /**
     * Probublá prvek (právě odebraný - první prvek) dolu tak dlouho, dokud není splněno haldové uspořádání
     * <i>(bubbleDown/heapifyDown/siftDown)</i>
     *
     * @see AbstrHeap#odeberMax()
     */
    private void probublejDolu() {
        int aktualniIndex = 0;

        while (!jeListem(aktualniIndex)) {
            final int levyIndex = indexLevehoSyna(aktualniIndex);
            final int pravyIndex = indexPravehoSyna(aktualniIndex);
            int nejvetsiIndex = aktualniIndex;

            if (levyIndex < mohutnost() &&
                    komparator.compare(halda[levyIndex],
                                       halda[nejvetsiIndex]) > 0)
                nejvetsiIndex = levyIndex;

            if (pravyIndex < mohutnost() &&
                    komparator.compare(halda[pravyIndex],
                                       halda[nejvetsiIndex]) > 0)
                nejvetsiIndex = pravyIndex;

            if (nejvetsiIndex != aktualniIndex) {
                vymen(aktualniIndex, nejvetsiIndex);
                aktualniIndex = nejvetsiIndex;
            } else {
                break;
            }
        }
    }

    /**
     * Probublá prvky od půlky pole na začátek
     *
     * @param aktualniIndex Pozice prvku (od půlky do začátku pole)
     * @param delkaPulky Délka poloviny pole
     */
    private void probublejPulku(int aktualniIndex, int delkaPulky) {
        while (aktualniIndex < delkaPulky) {
            final int levyIndex = indexLevehoSyna(aktualniIndex);
            final int pravyIndex = indexPravehoSyna(aktualniIndex);
            int nejvetsiIndex = aktualniIndex;

            if (levyIndex <= (mohutnost - 1) &&
                    komparator.compare(halda[levyIndex],
                                       halda[nejvetsiIndex]) > 0)
                nejvetsiIndex = levyIndex;

            if (pravyIndex <= (mohutnost - 1) &&
                    komparator.compare(halda[pravyIndex],
                                       halda[nejvetsiIndex]) > 0)
                nejvetsiIndex = pravyIndex;

            if (nejvetsiIndex != aktualniIndex) {
                vymen(aktualniIndex, nejvetsiIndex);
                aktualniIndex = nejvetsiIndex;
            } else {
                break;
            }
        }
    }

    /**
     * Zkontroluje, zda je prvek na požadované pozici listem (tzn. nemá žádné potomky)
     *
     * <p> Popis logiky:
     * <ul>
     * <li> <b>poz > (mohutnost / 2)</b>: Zkontroluje, zda je pozice uzlu větší než polovina aktuální mohutnosti.
     * Horní polovina > <b>mohutnost / 2</b>
     * <li> <b>poz <= mohutnost</b>: Zkontroluje, zda pozice uzlu nepřesahuje aktuální velikost haldy
     * </ul>
     *
     * <p> <b>Poznámka</b>: Listy v haldě jsou všechny uzly, jež se nacházejí v dolní polovině haldy
     *
     * @param poz Index prvku pro kontrolu
     *
     * @return {@code true} pokud je uzel na dané pozici listem, jinak {@code false}
     *
     * @see AbstrHeap#probublejDolu()
     */
    private boolean jeListem(int poz) {
        return poz > (mohutnost / 2) && poz <= mohutnost;
    }

    /**
     * Provádí výměnu prvků na zadaných pozicích
     *
     * @param poz1 Pozice <i>(position)</i> prvního prvku
     * @param poz2 Pozice <i>(position)</i> druhého prvku
     *
     * @see AbstrHeap#probublejNahoru()
     * @see AbstrHeap#probublejDolu()
     */
    private void vymen(int poz1, int poz2) {
        final E docasna = halda[poz1];
        halda[poz1] = halda[poz2];
        halda[poz2] = docasna;
    }
// </editor-fold>

//<editor-fold defaultstate="collapsed" desc="Privátní Metody: Přístup k prvkům">
    /**
     * Výpočet pozic levého syna, pravého syna a předka závisí na tom, jak jsou indexy pole <i>(případně uzlů v
     * binárním stromu)</i> číslovány. Jsou používány dvě konvence:
     * <ol>
     * <li> <b>Nula-indexované pole</b>:
     *      <ul>
     *      <li> Levý syn: {@code 2 * pos + 1}
     *      <li> Pravý syn: {@code 2 * pos + 2}
     *      <li> Předek: {@code (pos - 1) / 2}
     *      </ul>
     * <li> <b>Jedna-indexované pole</b>:
     *      <ul>
     *      <li> Levý syn: {@code 2 * pos}
     *      <li> Pravý syn: {@code 2 * pos + 1}
     *      <li> Předek: {@code pos / 2}
     *      </ul>
     * </ol>
     * <b>Poznámka</b>: V praxi je běžné používat <b>nula-indexované pole</b>, což odpovídá běžné praxi v
     * programování v Javě, jež indexuje pole <i>(array)</i> od nuly
     */
    private int indexLevehoSyna(int indexPredka) {
        return (2 * indexPredka) + 1;
    }

    private int indexPravehoSyna(int indexPredka) {
        return (2 * indexPredka) + 2;
    }

    private int indexPredka(int indexSyna) {
        return indexSyna / 2;
    }

    /**
     * Gettery pro předka a jeho syny
     */
    private E levySyn(int indexPredka) {
        return halda[indexLevehoSyna(indexPredka)];
    }

    private E pravySyn(int indexPredka) {
        return halda[indexPravehoSyna(indexPredka)];
    }

    private E predek(int indexSyna) {
        return halda[indexPredka(indexSyna)];
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Privátní Metody: Zjišťování existence předka/synů">
    private boolean jeLevySyn(int indexPredka) {
        return indexLevehoSyna(indexPredka) < mohutnost();
    }

    private boolean jePravySyn(int indexPredka) {
        return indexPravehoSyna(indexPredka) < mohutnost();
    }

    private boolean jePredek(int indexSyna) {
        return indexPredka(indexSyna) >= 0;
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Privátní Metody: Kopírování pole">
    /**
     * Zkopíruje prvky ze zdrojového <i>(source)</i> pole do cílového <i>(destination)</i> pole
     *
     * <p> <b>Poznámka</b>: Dalo by se použit {@link System#arraycopy(Object, int, Object, int, int)}
     *
     * @param zdrojove Zdrojové pole, z nějož se mají kopírovat prvky
     * @param cilove Cílové pole, do něhož se mají kopírovat prvky
     *
     * @throws HeapException Když jsou zdrojové a/nebo cílové pole prázdné, nebo mají různou délku
     */
    private void zkopirujPole(E[] zdrojove, E[] cilove) throws HeapException {
        pozadatNePrazdnaPole(zdrojove, cilove);
        pozadatStejnouDelkuPole(zdrojove, cilove);

        for (int i = 0; i < zdrojove.length; i++) {
            cilove[i] = zdrojove[i];
        }
    }
// </editor-fold>
}
