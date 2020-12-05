package Backend;

import Frontend.MainFrame;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

public class Facede {

    private static Facede instancia;

    private Maquina maquina = new Maquina();
    private PilhaDados pilhaDados = PilhaDados.getInstance();
    private PilhaPrograma pilhaPrograma = PilhaPrograma.getInstance();

    private boolean continuar = false;
    private boolean debug;
    private int parada;
    private ArrayList<String> input = new ArrayList();

    private Facede() {
    }

    public static Facede getInstance() {
        if (instancia == null) {
            instancia = new Facede();
        }
        return instancia;
    }

    public void executaProg(boolean debug, int parada) {
        this.debug = debug;
        this.parada = parada;

        new MaquinaThread().start();
    }

    public ArrayList obtemArquivo(String caminho) {
        LeArquivo dadosArquivo = new LeArquivo(caminho);
        dadosArquivo.ler();

        maquina.preenchePilhaPrograma(dadosArquivo.getDados());
        return dadosArquivo.getDados();
    }

    public ArrayList obtemPilhaDados() {
        return pilhaDados.getPilha();
    }

    public ArrayList obtemPilhaPrograma() {
        return pilhaPrograma.getPilha();
    }
    //erro
    public void atualizaTabelas() {
        MainFrame instanciaMainFrame = MainFrame.getInstance();
        instanciaMainFrame.atualizaPilhas();
    }

    public void reiniciaInstancias() {
        maquina = new Maquina();
        pilhaDados = PilhaDados.getInstance();
        pilhaPrograma = PilhaPrograma.getInstance();
    }

    public void cotinuaExecucao() {
        continuar = true;
    }

    public boolean verificaContinuacaoExe() {
        return continuar;
    }

    public void escritaDeDados(String dado) {
        MainFrame instanciaMainFrame = MainFrame.getInstance();
        instanciaMainFrame.escrita_out(dado);
    }

    public void insereInput(String dado)
    {
        System.out.println(dado);
        input.add(dado);
    }
    
    public String leInput()
    {
        String aux;
        
        if(input.size() == 0)
        {
            return null;
        }
        else
        {
            aux = input.get(0);
            input.remove(0);
            return aux;
        }
    }
    
    public class MaquinaThread extends Thread{
        public void run()
        {
            try {
                maquina.executaInstrucoes(debug, parada);
            } catch (InterruptedException ex) {
                Logger.getLogger(Facede.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
