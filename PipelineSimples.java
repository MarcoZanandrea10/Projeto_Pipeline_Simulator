import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PipelineSimples {
    public static void main(String[] args) throws IOException {
        // String arquivoEntrada = "instrucoes.txt";
        String arquivoEntrada = "fib_rec_hexadecimal.txt";

        // le todas as instruções do arquivo
        List<String> instrucoes = Files.readAllLines(Paths.get(arquivoEntrada));

        System.out.println("SIMULADOR DE PIPELINE");
        System.out.println("Instruções originais: " + instrucoes.size() + "\n");

        simularPipeline(instrucoes, false); // sem forwarding
        simularPipeline(instrucoes, true); // com forwarding
    }

    public static void simularPipeline(List<String> instrucoes, boolean forwarding) throws IOException {
        List<String> saida = new ArrayList<>(); // linhas de saída com endereços
        Map<Integer, String> mapa = new LinkedHashMap<>(); // mantém a ordem das instruções com endereços

        // contadores de conflitos e NOPs
        int conflitosDados = 0;
        int conflitosControle = 0;
        int nopsInseridos = 0;
        int endereco = 0;
        String regDestinoAnterior = "";

        // percorre todas as instruções
        for (String instrucao : instrucoes) {
            instrucao = instrucao.trim();
            if (instrucao.isEmpty()) continue;
                
            // verifica se é instrução em hexadecimal
            boolean Hex = instrucao.matches("^[0-9a-fA-F]{8}$");

            String rd = "";
            String rs = "";
            String rt = "";
            boolean instrucaoControle = false;

            // conflitos de controle
            if (Hex) {
                int inst = Integer.parseUnsignedInt(instrucao, 16);
                int opcode = inst & 0x7F;

                rd = "R" + ((inst >>> 7) & 0x1F);
                rs = "R" + ((inst >>> 15) & 0x1F);
                rt = "R" + ((inst >>> 20) & 0x1F);

                // verifica se é instrução de desvio
                if (opcode == 0x63 || opcode == 0x6F || opcode == 0x67) {
                    instrucaoControle = true;
                }

            } else {
                String semVirgulas = instrucao.replace(",", "");
                String[] partes = semVirgulas.split("\\s+");

                // obtém os registradores conforme o formato da instrução
                if (partes.length >= 2)
                    rd = partes[1];
                if (partes.length >= 3)
                    rs = partes[2];
                if (partes.length >= 4)
                    rt = partes[3];

                if (instrucao.startsWith("BEQ") || instrucao.startsWith("BNE") || instrucao.startsWith("J")) {
                    instrucaoControle = true;
                }
            }

            // trata instruções de dados
            if (!regDestinoAnterior.equals("") && (rs.equals(regDestinoAnterior) || rt.equals(regDestinoAnterior))) {
                conflitosDados++;

                // add os NOPs
                for (int i = 0; i < 3; i++) {
                    mapa.put(endereco, "NOP");
                    endereco += 4;
                    nopsInseridos++;
                }
            }

            mapa.put(endereco, instrucao);
            endereco += 4;

            // trata instruções de controle
            if (instrucaoControle) {
                conflitosControle++;

                for (int i = 0; i < 3; i++) {
                    mapa.put(endereco, "NOP");
                    endereco += 4;
                    nopsInseridos++;
                }

                regDestinoAnterior = "";
            } else {
                regDestinoAnterior = rd;
            }
        }

        // monta resultado com endereços
        for (Map.Entry<Integer, String> e : mapa.entrySet()) {
            String endHex = String.format("0x%04X", e.getKey());
            saida.add(endHex + "  " + e.getValue());
        }

        System.out.println("Resultado (" + (forwarding ? "Com" : "Sem") + " Forwarding)");
        System.out.println("Conflitos de Dados: " + conflitosDados);
        System.out.println("Conflitos de Controle: " + conflitosControle);
        System.out.println("NOPs Inseridos: " + nopsInseridos);
        System.out.println("Sobrecusto: +" + nopsInseridos + " instruções");
        System.out.println("Total final: " + (instrucoes.size() + nopsInseridos));
        System.out.println("Endereço final: 0x" + String.format("%04X", (mapa.size() * 4) - 4));
        System.out.println("\n--------------------------------------------\n");

        String nomeSaida = forwarding ? "saida_com_forwarding.txt" : "saida_sem_forwarding.txt";
        Files.write(Paths.get(nomeSaida), saida);
    }
}