/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analisador.sintatico;
import java.util.ArrayList;
import analisador.lexico.backend.AnalisadorLexico;
import analisador.lexico.backend.Token;
import analisador.semantico.AnalisadorSemantico;
import geracao.de.codigo.GeradorDeCodigo;

/**
 *
 * @author lucas
 */

public class AnalisadorSintatico extends Throwable{    
    Token token;
    ArrayList <Token> expressao = new ArrayList<>();
    int linhaErro = -1;
    int rotulo;
    int totalVariaveisAlocadas;
    int quantidadeVarParaAlocar;
    int nivelRetornoFuncao;//caso tenha outros inicio fim dentro de funcao(checagem e feita aqui)

    String mensagemErro;
    AnalisadorLexico analisadorLexico;
    AnalisadorSemantico analisadorSemantico;
    GeradorDeCodigo gera;
    boolean flagRetornoOk;
    boolean flagVerificaRetorno;
    String nomeArquivoFonte = "";

    
    public void inicioSintatico(String caminho, String nomeArquivo) throws Exception{
        linhaErro = -1;
        mensagemErro = "";
        analisadorLexico = new AnalisadorLexico(caminho);
        analisadorSemantico = new AnalisadorSemantico();
        gera = new GeradorDeCodigo();
        flagRetornoOk = false;
        flagVerificaRetorno = false;     
        quantidadeVarParaAlocar = 1;//Aqui e 1 pra allocar a posicao 0 para retorno de funcao
        totalVariaveisAlocadas = 0;
        rotulo = 1;
        nomeArquivoFonte = nomeArquivo;
        nivelRetornoFuncao = 0;
        analisadorSintatico();
    }

    private void getToken() throws Exception{
        token = analisadorLexico.getToken();
        if (!"".equals(token.getErro())){
            trataErro(token.getErro());
        }
    }
    
    private void analisadorSintatico() throws Exception{
        getToken();
        if ("sprograma".equals(token.getSimbolo())){
            getToken();
            if ("sidentificador".equals(token.getSimbolo())){
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                analisadorSemantico.insere_tabela(token.getLexema(), "nomedeprograma", "", -1);
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //Gera START
                System.out.println("START");
                gera.codigo("START");
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                
                getToken();
                if ("sponto_virgula".equals(token.getSimbolo())){
                   analisaBloco();
                   //getToken();
                   //System.out.println("token.getSimbolo() " + token.getSimbolo());
                   if ("sponto".equals(token.getSimbolo())){                       
                       getToken();
                       if ("".equals(token.getSimbolo())){
                           
                           //+++++++++++++++++++++++++++++++++++++++++++++++++++
                           //Gera HLT
                           System.out.println("HLT");
                           gera.codigo("HLT");
                           gera.salvaCodigo(nomeArquivoFonte);
                           //+++++++++++++++++++++++++++++++++++++++++++++++++++

                           //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                           //analisadorSemantico.imprimeTabelaSimbolos();
                           //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                           
                       }else trataErro("Caracteres apos fim do programa");  
                   }else{
                       trataErro("Esperado ponto");                       
                   }                            
                }else trataErro("Esperado pontovirgula");  
            }else trataErro("Esperado indentificador"); 
        }else trataErro("Esperada palvra reservada programa"); 
    }
    private void analisaBloco() throws Exception{
        int bkpVariaveisAlocadas = 0, bkpVariaveisParaAlocadar = 0;
        getToken();
        analisaEtVariaveis();  
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera ALLOC 0,quantidade de var
        if(quantidadeVarParaAlocar>0){
            System.out.println("ALLOC " + totalVariaveisAlocadas + "," + quantidadeVarParaAlocar);
            gera.codigo("ALLOC " + totalVariaveisAlocadas + "," + quantidadeVarParaAlocar);
            bkpVariaveisAlocadas = totalVariaveisAlocadas;//Salva quantidade atual para desalocar
            bkpVariaveisParaAlocadar = quantidadeVarParaAlocar;
            totalVariaveisAlocadas += quantidadeVarParaAlocar;
            quantidadeVarParaAlocar = 0;
        }
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        
        analisaSubRotina();
        analisaComandos();
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera DALLOC 0,quantidade de var
        if(bkpVariaveisParaAlocadar>0){
            System.out.println("DALLOC " + bkpVariaveisAlocadas + "," + bkpVariaveisParaAlocadar);
            gera.codigo("DALLOC " + bkpVariaveisAlocadas + "," + bkpVariaveisParaAlocadar);
            totalVariaveisAlocadas -= bkpVariaveisParaAlocadar;
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
    }
    private void analisaEtVariaveis() throws Exception{
        if ("svar".equals(token.getSimbolo())){
            getToken();
            if ("sidentificador".equals(token.getSimbolo())){                
                while("sidentificador".equals(token.getSimbolo())){
                    analisaVariaveis();
                    if ("sponto_virgula".equals(token.getSimbolo())){
                        getToken();                        
                    }else{
                        trataErro("Esperado pontovirgula"); 
                    }  
                }
            }else trataErro("Esperado indentificador");  
        }
    }
    private void analisaVariaveis() throws Exception{
        do{
            if ("sidentificador".equals(token.getSimbolo())){
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                if(analisadorSemantico.pesquisa_duplicvar_tabela(token.getLexema())){
                    trataErro("Variavel ja declarada");
                }
                analisadorSemantico.insere_tabela(token.getLexema(), "tipoVariavel", 
                        "", quantidadeVarParaAlocar + totalVariaveisAlocadas);
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                getToken();
                if ("svirgula".equals(token.getSimbolo()) || "sdoispontos".equals(token.getSimbolo())){
                    if ("svirgula".equals(token.getSimbolo())){
                        getToken();
                        if ("sdoispontos".equals(token.getSimbolo())) trataErro("Esperado identificador/variavel");
                    }
                }else{
                    trataErro("Esperado virgula ou doispontos");
                }
            }else{
                trataErro("Esperado identificador/variavel");
            }
            quantidadeVarParaAlocar++;
        }while (!"sdoispontos".equals(token.getSimbolo()));
        getToken();
        analisaTipo();        
    }
    private void analisaTipo() throws Exception{        
        if (!"sinteiro".equals(token.getSimbolo()) && !"sbooleano".equals(token.getSimbolo())){
            trataErro("Esperado inteiro ou booleano");
            return;
        }
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        analisadorSemantico.coloca_tipo(token.getLexema());
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        getToken();
    }
    private void analisaComandos() throws Exception{
        if ("sinicio".equals(token.getSimbolo())){
            getToken();
            
            //******************************************************************
            if(flagVerificaRetorno){
                flagRetornoOk=false;
                nivelRetornoFuncao++;
            }            
            //******************************************************************
            
            analisaComandoSimples();
            while (!"sfim".equals(token.getSimbolo())){               
                if ("sponto_virgula".equals(token.getSimbolo())){
                    getToken();
                    if (!"sfim".equals(token.getSimbolo())){
                        
                        //******************************************************
                        if(flagVerificaRetorno){
                            if (flagRetornoOk){
                                trataErro("Codigo unreachable apos retorno");
                            }                    
                        }
                        //******************************************************
                        
                        analisaComandoSimples(); 
                    }                                           
                } else{
                    trataErro("Esperado pontovirgula");
                }                
            }
            
            //******************************************************************
            // Se flagRetornoOk == false && fim da função então erro
            // Não encontrou um retorno
            if(flagVerificaRetorno){
                nivelRetornoFuncao--;
                if(!flagRetornoOk && nivelRetornoFuncao==0){
                    trataErro("Retorno nao encontrado");
                }
            } 
            //******************************************************************
            
            getToken();
        } else trataErro("Eperado palavra reservada inicio");
    }
    private void analisaComandoSimples() throws Exception{
        switch(token.getSimbolo()){
            case "sidentificador":
                
                //**************************************************************
                // O valor de retorno ou expressao tem que ter o mesmo tipo da 
                // funcao (analisa expressao verifica)
                // se nome "variavel" for igual nome da função corrente
                //      Então analisa atribuição e se tiver mais codigo depois = erro
                if(token.getLexema().equals(analisadorSemantico.getProcCorrente())){
                    flagRetornoOk = true;
                }
                //**************************************************************
                
                analisaAtribChProcedimento();
                break;
            case "sse":
                analisaSe();
                break;
            case "senquanto":
                analisaEnquanto();
                break;
            case "sleia":
                analisaLeia();
                break;
            case "sescreva":
                analisaEscreva();
                break;
            default:
                analisaComandos(); 
        }
    }
    private void analisaAtribChProcedimento() throws Exception{//Momento de utilizacao (consultas na tabela)
        //Salva token anterior antes de pegar o proximo
        Token bkpToken = new Token(token.getLexema(), token.getSimbolo(), 0); 
        getToken();        
        if ("satribuicao".equals(token.getSimbolo())){            
            //******************************************************************
            //Pode ir pra dentro do analisa atribuição 
            //Verificar se o token anterior eh variavel
            if(!analisadorSemantico.pesquisa_declvar_tabela(bkpToken.getLexema())){
                if(!flagVerificaRetorno){
                    trataErro("Atribuicao invalida");//Tentativa de atribuicao a nao variavel
                }else if(!analisadorSemantico.pesquisa_declfunc_tabela(bkpToken.getLexema())){
                        trataErro("Funcao nao declarada");
                        return;
                }
            }
            //******************************************************************
            analisaAtribuicao(bkpToken);
        }else{
            chamadaProcedimento(bkpToken);
        } 
    }
    private void analisaLeia() throws Exception{//So pode ler inteiros
        int indice;
        getToken();
        if ("sabre_parenteses".equals(token.getSimbolo())){
            getToken();
            if ("sidentificador".equals(token.getSimbolo())){
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                if(!analisadorSemantico.pesquisa_declvar_tabela(token.getLexema())){
                    trataErro("Variavel nao declarada");
                    return;
                }                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //gera RD
                System.out.println("RD");
                gera.codigo("RD");
                //gera STR                
                indice = analisadorSemantico.pesquisa_tabela(token.getLexema());
                System.out.println("STR " + analisadorSemantico.buscaMemoriaRotulo(indice));
                gera.codigo("STR " + analisadorSemantico.buscaMemoriaRotulo(indice));
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                
                getToken();
                if ("sfecha_parenteses".equals(token.getSimbolo())){
                    getToken();
                }else trataErro("Esperado fecha parentese"); 
            }else trataErro("Esperado variavel");              
        }else trataErro("Esperado abre parentese"); 
    }
    private void analisaEscreva() throws Exception{
        int indice;
        getToken();
        if ("sabre_parenteses".equals(token.getSimbolo())){
            getToken();
            if ("sidentificador".equals(token.getSimbolo())){
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                if(!analisadorSemantico.pesquisa_declvar_tabela(token.getLexema())){
                    trataErro("Variavel nao declarada");
                    return;
                }
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //gera LDV
                indice = analisadorSemantico.pesquisa_tabela(token.getLexema());
                System.out.println("LDV " + analisadorSemantico.buscaMemoriaRotulo(indice));
                gera.codigo("LDV " + analisadorSemantico.buscaMemoriaRotulo(indice));
                //gera PRN
                System.out.println("PRN");
                gera.codigo("PRN");
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                
                getToken();
                if ("sfecha_parenteses".equals(token.getSimbolo())){
                    getToken();
                }else trataErro("Esperado fecha parentese"); 
            }else trataErro("Esperado identificador");  
        }else trataErro("Esperado abre parentese"); 
    }
    private void analisaEnquanto() throws Exception{
        int retorno;
        int auxRot1, auxrot2;
        getToken();
        
    //--------------------------------------------------------------------------        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera L1 NULL
        auxRot1=rotulo;
        System.out.println("L" +rotulo+" NULL");
        gera.codigo("L" +rotulo+" NULL");
        rotulo++;
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
        
        retorno = analisaExpressaoRetorno(); 
        if(retorno == -1){
            token.setLinha(linhaErro);
            trataErro("Expressao com tipos difentes");
        }else if(retorno != 0){
            token.setLinha(linhaErro);
            trataErro("Expressao nao booleana");
        }
    //--------------------------------------------------------------------------
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera JUMPF L2
        auxrot2 = rotulo;
        System.out.println("JMPF L" + rotulo);
        gera.codigo("JMPF L" + rotulo);
        rotulo++;
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++  
        
        if ("sfaca".equals(token.getSimbolo())){       
            getToken();
            analisaComandoSimples(); 
            
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //gera JMP L1
            System.out.println("JMP L" + auxRot1);
            gera.codigo("JMP L" + auxRot1);
            //Gera L2 NULL
            System.out.println("L" +auxrot2+ " NULL");
            gera.codigo("L" +auxrot2+ " NULL");
            //rotulo++;
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            
            //******************************************************************
            //Retorno dentro do while nao eh garantido
            if(flagVerificaRetorno){
                flagRetornoOk = false;
            }    
            //******************************************************************
            
        }else trataErro("Esperado palavra reservada faca"); 
    }     
    private void analisaSe() throws Exception{
        int retorno, auxRot1, auxRot2;
        boolean backupflagRetornoOk = false;
        getToken();
        
        //----------------------------------------------------------------------
        retorno = analisaExpressaoRetorno();        
        if(retorno == -1){
            token.setLinha(linhaErro);
            trataErro("Expressao com tipos difentes");
            return;
        }else if(retorno != 0){
            token.setLinha(linhaErro);
            trataErro("Expressao nao booleana");
            return;
        }
        //----------------------------------------------------------------------
      
        if ("sentao".equals(token.getSimbolo())){
            
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Gera JMPF L1
            auxRot1 = rotulo;
            System.out.println("JMPF L" + rotulo);
            gera.codigo("JMPF L" + rotulo);
            rotulo++;
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            
            getToken();
            analisaComandoSimples();
            
            //******************************************************************
            if(flagVerificaRetorno){
                backupflagRetornoOk = flagRetornoOk;
                flagRetornoOk = false;
            }            
            //******************************************************************
            
            if ("ssenao".equals(token.getSimbolo())){ 
                
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Gera JMP L2
                auxRot2 = rotulo;
                System.out.println("JMP L" + rotulo);
                gera.codigo("JMP L" + rotulo);
            //Gera label L1
                System.out.println("L" +auxRot1+ " NULL");
                gera.codigo("L" +auxRot1+ " NULL");
                rotulo++;
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
            
                getToken();
                analisaComandoSimples(); 
                
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Gera label L2
            System.out.println("L" +auxRot2+ " NULL");
            gera.codigo("L" +auxRot2+ " NULL");
            rotulo++;
            }else{           
            //Gera label L1
            System.out.println("L" +auxRot1+ " NULL");
            gera.codigo("L" +auxRot1+ " NULL");
            rotulo++;
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            
            }
        }else trataErro("Esperado palavra reservada entao");
        
        //**********************************************************************
        //se backupflagRetornoOk ou flagRetornoOk != true entao
        //senao flagRetornoOk = true
        if(flagVerificaRetorno){
            if(!backupflagRetornoOk || !flagRetornoOk){
                flagRetornoOk = false;
            }else{
                flagRetornoOk = true;
            }
        }        
        //**********************************************************************
    }
    private void analisaSubRotina() throws Exception{
        int flag = 0;
        int auxrot=0;
        if ("sprocedimento".equals(token.getSimbolo()) || "sfuncao".equals(token.getSimbolo())){
            
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            auxrot= rotulo;                                                  
            //GERA(´ ´,JMP,rotulo,´ ´) {Salta sub-rotinas}                   
            System.out.println("JMP L" + rotulo);                            
            gera.codigo("JMP L" + rotulo);
            rotulo++;
            flag = 1;
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            
        }
        while("sprocedimento".equals(token.getSimbolo()) || "sfuncao".equals(token.getSimbolo())){
            if ("sprocedimento".equals(token.getSimbolo())){
                analisaDeclaracaoProcedimento();
            }else{
                analisaDeclaracaoFuncao();
            }
            if ("sponto_virgula".equals(token.getSimbolo())){
                getToken();
            }else {
                trataErro("Esperado pontovirgula");
                return;
            }  
        }
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //se flag = 1 entao Gera salto para "main" para pular declaracao de proc func
        if(flag == 1){
            System.out.println("L" + auxrot + " NULL");
            gera.codigo("L" + auxrot + " NULL");
        } 
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }
    private void analisaDeclaracaoProcedimento() throws Exception{
        getToken();
        if ("sidentificador".equals(token.getSimbolo())){
            
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            if(analisadorSemantico.pesquisa_declproc_tabela(token.getLexema())){
                trataErro("Procedimento ja declarado");
                return;
            }else{                
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //gera label NULL para chamada de proc
                System.out.println("L" +rotulo+ " NULL");
                gera.codigo("L" +rotulo+ " NULL");
                analisadorSemantico.insere_tabela(token.getLexema(), "tipoProcedimento", "L", rotulo);
                rotulo++;
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
            
            getToken();
            if("sponto_virgula".equals(token.getSimbolo())){
                analisaBloco();
            }else trataErro("Esperado pontovirgula");  
        }else trataErro("Esparado identificado"); 
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        analisadorSemantico.desempilha();        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera RETURN
        gera.codigo("RETURN");
        System.out.println("RETURN");
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }
    private void analisaDeclaracaoFuncao() throws Exception{
        getToken();
        if ("sidentificador".equals(token.getSimbolo())){
            
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            if(analisadorSemantico.pesquisa_declvarfunc_tabela(token.getLexema())){
                trataErro("Funcao ja declarada");
                return;
            }else{
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //gera label NULL para chamada de funcao
                System.out.println("L" +rotulo+ " NULL");
                gera.codigo("L" +rotulo+ " NULL");
                analisadorSemantico.insere_tabela(token.getLexema(), "tipoFuncao", "L", rotulo);
                rotulo++;
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            
            getToken();
            if ("sdoispontos".equals(token.getSimbolo())){
                getToken();
                if ("sinteiro".equals(token.getSimbolo()) || "sbooleano".equals(token.getSimbolo())){
                    
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    analisadorSemantico.coloca_tipo(token.getLexema());
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
                    
                    getToken();
                    if ("sponto_virgula".equals(token.getSimbolo())){
                        
                        //******************************************************
                        flagVerificaRetorno = true;
                        analisaBloco();
                        flagVerificaRetorno = false;
                        //****************************************************** 
                        
                    }
                }else trataErro("O tipo da funcao deve ser inteiro ou booleano");  
            }else trataErro("Esperado doispontos");  
        }else trataErro("Esperado identifacador");
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        analisadorSemantico.desempilha();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera RETURN
        gera.codigo("RETURN");
        System.out.println("RETURN");
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }
    private int  analisaExpressaoRetorno() throws Exception{
        expressao.clear();      
        analisaExpressao();//Gera a expressao original
        ArrayList<Token> exprecaoPosFixa;
        //System.out.println("Expressao original: ");
        //imprimeExpressao(expressao);
        exprecaoPosFixa = analisadorSemantico.convertePosFixa(expressao);
        if(exprecaoPosFixa.isEmpty()){
            trataErro("Procedimento nao retorna valores");//Proc dentro de expressao 
        }        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera expressao (codigo)
        for(int i=0 ; i< exprecaoPosFixa.size(); i++){
            if(exprecaoPosFixa.get(i).getLinha() < 0){
                System.out.println(exprecaoPosFixa.get(i).getSimbolo());
                gera.codigo(exprecaoPosFixa.get(i).getSimbolo());
            }else{
                System.out.println(exprecaoPosFixa.get(i).getSimbolo() +
                        exprecaoPosFixa.get(i).getLinha());
                gera.codigo(exprecaoPosFixa.get(i).getSimbolo() + exprecaoPosFixa.get(i).getLinha());
            }
        }
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        
        return analisadorSemantico.compatibilizacaoTipos(exprecaoPosFixa);//Retorna o tipo da expressao -1-erro 0-Bool 1-int     
    }
    private void analisaExpressao() throws Exception{
        linhaErro = token.getLinha();
        analisaExpressaoSimples();
        if (token.getSimbolo().equals("smenor") ||
                token.getSimbolo().equals("sdif") ||
                token.getSimbolo().equals("smenorig") ||
                token.getSimbolo().equals("smaior") ||
                token.getSimbolo().equals("smaiorig") ||
                token.getSimbolo().equals("sig")){
            
            //------------------------------------------------------------------
            expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
            //------------------------------------------------------------------
            
            getToken();
            analisaExpressaoSimples();
        }        
        //?O retorno do analisaExpressao nao pode ser aqui por causa da "recursao"
    }
    private void analisaExpressaoSimples() throws Exception{
        if ("smais".equals(token.getSimbolo()) || "smenos".equals(token.getSimbolo())){
            
            //------------------------------------------------------------------
            expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
            //------------------------------------------------------------------
            
            getToken();
        }
        analisaTermo();
        while ("smenos".equals(token.getSimbolo()) || 
                "smais".equals(token.getSimbolo()) ||
                "sou".equals(token.getSimbolo())){
            
            //------------------------------------------------------------------
            expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
            //------------------------------------------------------------------
            
            getToken();
            analisaTermo();
        }
    }
    private void analisaTermo() throws Exception{
        analisaFator();
        while("smult".equals(token.getSimbolo()) ||
                "sdiv".equals(token.getSimbolo()) ||
                "se".equals(token.getSimbolo())){
            
            //------------------------------------------------------------------
            expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
            //------------------------------------------------------------------
            
            getToken();
            analisaFator();
        }        
    }
    private void analisaFator() throws Exception{
        int indice;
        
        //----------------------------------------------------------------------
        expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
        //----------------------------------------------------------------------
        
        if ("sidentificador".equals(token.getSimbolo())){
            
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            indice = analisadorSemantico.pesquisa_tabela(token.getLexema());
            if(indice >= 0){
                if(analisadorSemantico.pesquisa_declfunc_tabela(token.getLexema()) &&
                   ("inteiro".equals(analisadorSemantico.buscaTipo(indice)) ||
                   "booleano".equals(analisadorSemantico.buscaTipo(indice)))){
                   analisaChamadaDeFuncao(indice);
                }else{
                    getToken(); 
                }
                
            }else{
                trataErro("Identificador nao definido");
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
            
        }else if ("snumero".equals(token.getSimbolo())){
                getToken();            
        }else if ("snao".equals(token.getSimbolo())){
            getToken();
            analisaFator();
        }else if ("sabre_parenteses".equals(token.getSimbolo())){
            getToken();
            analisaExpressao();
            if ("sfecha_parenteses".equals(token.getSimbolo())){
                
                //--------------------------------------------------------------
                expressao.add(new Token(token.getLexema(), token.getSimbolo(), 0));
                //--------------------------------------------------------------
                
                getToken();
            }else trataErro("Esperado fecha parentese");
        }else if ("verdadeiro".equals(token.getLexema()) || "falso".equals(token.getLexema())){
            getToken();
        }else trataErro("Esperado identficador, numero, parentese ou valor booleano");
    }
    private void analisaChamadaDeFuncao(int indice) throws Exception{
        //Geração de codigo foi para dentro da geração da pos fixa
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera CALL 
        //System.out.println("CALL " + analisadorSemantico.buscaMemoriaRotulo(indice));
        //System.out.println("LDV 0");
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++        
        getToken();
    }    
    private void chamadaProcedimento(Token tokenAnterior) throws Exception{
        int indice;        
        //Verifica se o token anterior e funcao, se for, erro
        if(analisadorSemantico.pesquisa_declfunc_tabela(tokenAnterior.getLexema())){
            trataErro("Funcao usada sem considerar o valor de retorno");                
        }
        //Verifica se o token anterior e procedimento, se nao for, erro 
        if(!analisadorSemantico.pesquisa_declproc_tabela(tokenAnterior.getLexema())){
            trataErro("Procedimento nao declarado");
        }
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        indice = analisadorSemantico.pesquisa_tabela(tokenAnterior.getLexema());
        gera.codigo("CALL L" + analisadorSemantico.buscaMemoriaRotulo(indice));
        System.out.println("CALL L" + analisadorSemantico.buscaMemoriaRotulo(indice));
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }
    private void analisaAtribuicao(Token tokenAnterior) throws Exception{        
        getToken();
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        int indice;
        String tipo;
        int tipoVariavel = -1;
        int tipoExpressao;

        indice = analisadorSemantico.pesquisa_tabela(tokenAnterior.getLexema());
        if(indice >= 0){
            tipo = analisadorSemantico.buscaTipo(indice);
            if("inteiro".equals(tipo)){
                tipoVariavel = 1;
            }else if("booleano".equals(tipo)){
                tipoVariavel = 0;
            }else{
                trataErro("Variavel de atribuicao nao e int ou bool");
            }
        }else{
            trataErro("Variavel nao declrada");
        }
        
        tipoExpressao = analisaExpressaoRetorno();
        
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gera STR
        if(flagVerificaRetorno && analisadorSemantico.pesquisa_declfunc_tabela(tokenAnterior.getLexema())){
            gera.codigo("STR 0");
            System.out.println("STR 0");
        }else{
            gera.codigo("STR " + analisadorSemantico.buscaMemoriaRotulo(indice));
            System.out.println("STR " + analisadorSemantico.buscaMemoriaRotulo(indice));
        }
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        
        if(tipoExpressao == -1){            
            token.setLinha(linhaErro);            
            trataErro("Expressao com tipos difentes!");
        }else if(tipoExpressao != tipoVariavel){
            token.setLinha(linhaErro);
            trataErro("Atribuicao com tipos difentes");
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    }  
    private void trataErro(String metodoChamouErro) throws Exception{
        throw new Exception(token.getLinha() + " - " + metodoChamouErro);
    }

    private void imprimeExpressao(ArrayList<Token> expressao){//Metodo temporario
        for(int i = 0; i< expressao.size(); i++){
            System.out.print(expressao.get(i).getLexema());
        }
        System.out.println("");
    }
}

