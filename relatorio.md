# RELATÓRIO EP1 - SISTEMAS DISTRIBUIDOS

### Vinicius Santana Santos - RA: 11201811841

***
O video demonstrando o funcionamento pode ser acessado pelo link: https://youtu.be/f0w85jh5bCE

## O src.Servidor

Para iniciar o servidor, basta executar o comando `java src.Servidor <ip> <porta>`. O servidor já possui um ip e porta
padrão caso nenhum seja passado como argumento. O servidor é implementado na classe src.Servidor, que possui um método
main
que recebe os argumentos e inicia o registry RMI.

A implementação dos métodos do nosso serviço RMI estão na classe ServerServiceImpl, que implementa a interface
ServerService. O serviço tem 4 métodos:

* **registerPeer**: Recebe ipAddress, porta e uma lista de arquivos e registra o peer no servidor. caso o
  peer já esteja registrado, ele retorna um JOIN_ALREADY ao invés de JOIN_OK. (JOIN)
* **unregisterPeer**: Recebe ipAddress e porta e remove o peer do servidor. Este método é usado quando um src.Peer é
  desligado.
* **searchFile**: Recebe o nome do arquivo e retorna uma lista de peers que possuem o arquivo. Caso não exista nenhum
  peer com o arquivo, retorna uma lista vazia. (SEARCH)
* **updateFiles**: Recebe uma lista de arquivos e atualiza a lista de arquivos do peer. Este método é chamado quando um
  peer adiciona ou remove um arquivo a sua pasta. (UPDATE)

Para guardar os metadados dos peers, foi criado um ConcurrentHashMap, onde a chave é o ip e a porta do peer e o valor é
um objeto
chamado Files definido na linha 56 que apenas contém um Vector que guarda os nomes dos arquivos como atributo e um
método para checar se um arquivo existe naquele Vector.

```java 
ConcurrentHashMap<String, Files> peers=new ConcurrentHashMap<String, Files>();

public static class Files {

    Vector<String> files;

    public Files(Vector<String> files) {
        this.files = files;
    }

    public boolean contains(String fileName) {
        return files.contains(fileName);
    }
}
```

## O src.Peer

Para iniciar o peer, basta executar o comando `java src.Peer <ip> <porta> <dirPath>` onde dirPath é o caminho absoluto
para
a pasta
que contém os arquivos do peer.

O peer é implementado na classe src.Peer, que possui um método main que recebe os argumentos, inicia o registry RMI,
cria o
stub do servidor, checa para ver a pasta passada nos argumentos existe, pega os nomes dos arquivos na pasta e inicia o
CLI (menu interativo).

### JOIN (linha 48)

Para dar join na rede P2P, digite `JOIN <ip> <porta> <dirPath>` no CLI. É feita uma checagem para ver se as informações
passadas na inicialização do peer são iguais as passadas no JOIN.
Após isso, o peer chama o método registerPeer do servidor, passando seu ip, porta e a lista de arquivos que ele possui.
Se receber JOIN_OK, o peer inicia uma thread com um server que fica escutando por conexões de outros peers, um shutdown
hook que chama o método unregisterPeer quando o src.Peer é desligado, e um fileWacher que fica observando a
pasta do peer e chama o método updateFiles quando um arquivo é adicionado ou removido. Mas se receber JOIN_ALREADY, o
peer já está registrado e todos os JOINs subsequentes são ignorados.

### SEARCH (linha 89)

Para fazer uma busca por um arquivo, digite `SEARCH <fileName>` no CLI. O peer chama o método searchFile do servidor e
printa os endereços dos peers que possuem o arquivo e se receber um array vazio, printa uma mensagem dizendo que o
arquivo não foi
encontrado.

### DOWNLOAD (linha 103)

Para fazer o download de um arquivo, digite `DOWNLOAD <ip> <porta>` no CLI. O peer cria uma conexão TCP com o peer que
possui o último arquivo pesquisado em uma thread e recebe o arquivo. O arquivo é salvo na pasta do peer.

## Multithreading

O servidor é threadsafe, pois usamos concurrentHashMap e synchronized. O peer é multithreaded, pois a
thread principal cuida do CLI, uma thread cuida do server, uma thread cuida do fileWatcher e uma thread cuida do
shutdownHook e toda vez que o comando DOWNLOAD é escolhido, uma thread é criada para fazer o download.

### ServerThread (linha 132)

Esta thread é criada quando o peer dá JOIN na rede. Ela fica escutando por conexões de outros peers e quando uma conexão
acontece, ela cria uma outra thread (ServerHandler) para lidar com essa conexão, ou seja, temos uma thread só para lidar
com conexões e uma p para lidar com downloads/uploads de arquivos.

### ServerHandler (linha 162)

Esta thread é criada quando uma conexão é estabelecida entre dois peers. Ela recebe o nome do arquivo que o peer Client,
checa se o arquivo existe na pasta do peer Server e se existir, envia o arquivo para o peer Client.

### ClientThread (linha 227)

Esta thread é criada quando o peer quer fazer download de um arquivo. Ela cria uma conexão TCP com o peer que possui o
arquivo, espera por uma mensagem de erro ou sucesso e se receber sucesso, recebe o arquivo e salva na pasta do peer.

### FileWatcher (linha 292)

Esta thread é criada quando o peer dá JOIN na rede. Ela fica observando a pasta do peer e quando um arquivo é adicionado
ou removido, ela chama o método updateFiles do servidor. O FileWatcher utiliza a classe WatchService do java.nio para
observar a pasta do peer e a classe WatchEvent para pegar os eventos de criação e remoção de arquivos. O registro desse
watchService é feito na linha 307.

## Transferência de arquivos

A transferência de arquivos é feita usando sockets TCP. Quando um peer quer fazer download de um arquivo, ele cria uma
conexão TCP com o peer que possui o arquivo e envia o nome do arquivo. O peer que possui o arquivo checa se o arquivo
existe na pasta e se existir, envia o arquivo para o peer que fez a requisição. O arquivo é enviado em chunks de 4096
bytes. A seguir podemos ver a lógica de download de arquivos:

```java
 File file=new File(folderPath+FileSystems.getDefault().getSeparator()+fileName);

        FileOutputStream fos=new FileOutputStream(file);
        InputStream is=socket.getInputStream();

        byte[]buffer=new byte[4096];
        int bytesRead;
        while((bytesRead=is.read(buffer))!=-1){
        fos.write(buffer,0,bytesRead);
        }

        fos.close();
        is.close();
        socket.close();
        System.out.println("Arquivo "+fileName+" baixado com sucesso na pasta "+folderPath);
```

E a lógica de upload de arquivos.

```java
 File file=new File(filePath+FileSystems.getDefault().getSeparator()+fileName);
        FileInputStream fis=new FileInputStream(file);
        OutputStream os=clientSocket.getOutputStream();

        byte[]buffer=new byte[4096];
        int bytesRead;
        while((bytesRead=fis.read(buffer))!=-1){
        os.write(buffer,0,bytesRead);
        }

        os.flush();
        os.close();
        fis.close();
        clientSocket.close();
```

Enviar o arquivo em chunks traz vários benefícios, como por exemplo, não precisar alocar muita memória para enviar um
arquivo muito grande.

## Conclusão

O projeto foi muito interessante, pois pudemos aprender sobre RMI, sockets TCP, multithreading, transferência de
arquivos, redes P2P e Java. Através desse projeto, também foi possível enxergar pequenos aspectos do funcionamento de um
sistema distribuído, como comunicação assíncrona, tolerância a falhas, stateful/stateless, etc. Por mais que esse
projeto seja
considerado uma *toy application*, ele nos deu uma boa noção de como um sistema distribuído funciona.

## Algumas considerações

Parte do código foi construído com ajuda do ChatGPT e o github copilot, principalmente para descobrir alguns
aspectos desconhecidos da linguagem, como o watchService. Os vídeos do canal do youtube do professor também foram de
grande ajuda.