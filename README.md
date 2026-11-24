# Missãolive Android - MVP inicial

Este protótipo abre a câmera Android, mostra uma prévia e, ao iniciar, disponibiliza um endpoint MJPEG na rede local:

`http://IP_DO_CELULAR:8080/video.mjpg`

## Testar no OBS Studio
1. Instale o APK (após compilar) no Android e permita acesso à câmera.
2. Celular e computador devem estar na mesma rede Wi-Fi.
3. Toque em **INICIAR TRANSMISSÃO** e copie o endereço mostrado.
4. No OBS, tente adicionar uma fonte **Navegador (Browser)** e use o URL como página. Alternativamente, abra o URL em um navegador do PC para validar.

## Compilar
Abra a pasta no Android Studio, sincronize o Gradle e compile o APK.

## Limitações deste protótipo
- A opção 4K solicita 3840x2160 à câmera; aparelhos que não suportam essa resolução podem escolher outra resolução ou falhar. O 4K ainda precisa de testes de hardware e performance.
- O MJPEG é uma prova de conceito, não é otimizado para bitrate, latência ou uso prolongado. Ele consome bastante banda e CPU; WebRTC ou H.264 serão melhorias para versões seguintes.
- USB ainda não implementado. A versão inicial funciona por Wi-Fi/LAN.
- O receptor Browser do OBS pode não renderizar multipart MJPEG de modo uniforme em todas as versões; um plugin/receptor dedicado ou saída H.264/WebRTC é um próximo passo.
- Não mantenha este servidor aberto em redes públicas: ele não tem autenticação.
