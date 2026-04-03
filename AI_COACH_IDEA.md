# Idea futura: AI Coach (chat + genera scheda + progressione)

Obiettivo: integrare una IA tipo personal trainer.
- Chat con cui parlare
- Bottone "genera scheda"
- Usa anche i log allenamenti per suggerire progressioni
- Preferenza: gratis se possibile, ok pochi euro/mese per servizio professionale

Vincoli/Scelte tecniche:
- NON mettere API key nell'app Android
- Usare backend serverless (consigliato: Firebase Cloud Functions + Firebase Auth)
- MVP: chat + generazione scheda (testo + JSON)
- Step 2: progressione basata su summary dei log (ultimi 14–28 giorni, PR, trend)
