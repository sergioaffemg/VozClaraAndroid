# Voz Clara — protótipo Android

Protótipo Kotlin para captação do microfone e reprodução em tempo real:
- PCM mono a 48 kHz com AudioRecord/AudioTrack;
- tenta ativar o NoiseSuppressor do Android, se disponível;
- controle inicial de ganho (70% é escala interna, não nível auditivo calibrado);
- saída roteada pelo Android para o dispositivo de áudio ativo.

## Abrir
Abra esta pasta no Android Studio com JDK 17, sincronize Gradle e execute em aparelho físico Android. Conceda a permissão de microfone e conecte o fone com fio/USB-C.

## Limitações
É um protótipo técnico, não um aparelho auditivo validado. A qualidade do NoiseSuppressor varia por fabricante; não há separação de voz por IA nem garantia de redução de ruído. O áudio em loopback pode ter atraso, distorção ou realimentação; use fones, comece em volume baixo e interrompa se houver desconforto. O Android seleciona a saída ativa automaticamente. O uso contínuo com tela apagada exigirá implementar um foreground service.

Próximas etapas: detectar o dispositivo conectado, medir latência, acrescentar filtros de voz e limitador suave, e avaliar um modelo de redução de ruído local.
