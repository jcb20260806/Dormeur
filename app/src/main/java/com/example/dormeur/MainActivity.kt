package com.example.dormeur

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var txtDormeur: TextView
    private lateinit var btnMasquer: Button
    private lateinit var btnAfficher: Button
    private lateinit var btnRemasquer: Button
    private lateinit var btnReset: Button
    private lateinit var btnOptions: Button

    // Texte actuellement affiché
    private var texte = ""

    // ---------------------------------------------------------
    // Élément de la pile
    // ---------------------------------------------------------

    data class MotMasque(
        val debut: Int,
        val fin: Int,
        val mot: String
    )

    // La pile des mots masqués
    // Last In First Out
    private val pile = ArrayDeque<MotMasque>()

    // Dernier mot qui vient d'être réaffiché
    private var dernierMotAffiche: MotMasque? = null

    // ---------------------------------------------------------
    // Sauvegarde
    // ---------------------------------------------------------

    private val preferences by lazy {
        getSharedPreferences("Dormeur", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        txtDormeur = findViewById(R.id.txtDormeur)
        btnMasquer = findViewById(R.id.btnMasquer)
        btnAfficher = findViewById(R.id.btnAfficher)
        btnRemasquer = findViewById(R.id.btnRemasquer)
        btnReset = findViewById(R.id.btnReset)
        btnOptions = findViewById(R.id.btnOptions)

        // -----------------------------------------------------
        // Restaurer automatiquement l'état précédent
        // -----------------------------------------------------

        restaurerEtat()

        // -----------------------------------------------------
        // Bouton MASQUER
        // -----------------------------------------------------

        btnMasquer.setOnClickListener {
            masquerMotAleatoire()
        }

        // -----------------------------------------------------
        // Bouton AFFICHER
        // -----------------------------------------------------

        btnAfficher.setOnClickListener {
            afficherDernierMot()
        }

        // -----------------------------------------------------
        // Bouton REMASQUER
        // -----------------------------------------------------

        btnRemasquer.setOnClickListener {
            remasquerDernierMot()
        }

        // -----------------------------------------------------
        // Bouton RESET
        // -----------------------------------------------------

        btnReset.setOnClickListener {
            resetTexte()
        }

        // -----------------------------------------------------
        // Bouton OPTIONS
        // -----------------------------------------------------

        btnOptions.setOnClickListener {
            startActivity(
                Intent(this, OptionsActivity::class.java)
            )
        }
    }

    // =========================================================
    // AFFICHAGE DU TEXTE
    // =========================================================

    private fun afficherTexte() {

        val spannable = SpannableString(texte)

        var debutLigne = 0

        for (i in texte.indices) {

            if (texte[i] == '\n') {

                colorerPremiereLettre(
                    spannable,
                    debutLigne,
                    i
                )

                debutLigne = i + 1
            }
        }

        // Dernière ligne
        colorerPremiereLettre(
            spannable,
            debutLigne,
            texte.length
        )

        // -----------------------------------------------------
        // Colorer les tirets en rose
        // -----------------------------------------------------

        for (i in texte.indices) {

            if (texte[i] == '-') {

                spannable.setSpan(
                    ForegroundColorSpan(
                        Color.rgb(255, 105, 180)
                    ),
                    i,
                    i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        txtDormeur.text = spannable
    }

    // =========================================================
    // PREMIÈRE LETTRE DE CHAQUE LIGNE EN ROUGE
    // =========================================================

    private fun colorerPremiereLettre(
        spannable: SpannableString,
        debut: Int,
        fin: Int
    ) {

        for (i in debut until fin) {

            if (!texte[i].isWhitespace()) {

                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    i,
                    i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                break
            }
        }
    }

    // =========================================================
    // MASQUER UN MOT
    // =========================================================

    private fun masquerMotAleatoire() {

        val regex = Regex("\\b[\\p{L}\\p{N}]+\\b")
        val mots = regex.findAll(texte).toList()

        if (mots.isEmpty()) {
            return
        }

        // Choisir un mot au hasard
        val mot = mots[Random.nextInt(mots.size)]

        // Sauvegarder le mot dans la pile
        val motMasque = MotMasque(
            debut = mot.range.first,
            fin = mot.range.last + 1,
            mot = mot.value
        )

        pile.addLast(motMasque)

        // -----------------------------------------------------
        // Conserver la première lettre
        // -----------------------------------------------------

        val premiereLettre = mot.value.first()

        val tirets = "-".repeat(
            mot.value.length - 1
        )

        texte = texte.replaceRange(
            mot.range,
            premiereLettre + tirets
        )

        // Un nouveau masquage annule le dernier
        // mot réaffiché
        dernierMotAffiche = null

        afficherTexte()

        // Sauvegarde automatique
        sauverEtat()
    }

    // =========================================================
    // RÉAFFICHER LE DERNIER MOT MASQUÉ
    // =========================================================

    private fun afficherDernierMot() {

        if (pile.isEmpty()) {
            return
        }

        // LIFO :
        // dernier mot ajouté = premier mot réaffiché
        val dernier = pile.removeLast()

        texte = texte.replaceRange(
            dernier.debut,
            dernier.fin,
            dernier.mot
        )

        // Mémoriser le dernier mot réaffiché
        dernierMotAffiche = dernier

        afficherTexte()

        // Sauvegarde automatique
        sauverEtat()
    }

    // =========================================================
    // REMASQUER LE MOT QUI VIENT D'ÊTRE RÉAFFICHÉ
    // =========================================================

    private fun remasquerDernierMot() {

        val dernier = dernierMotAffiche ?: return

        // Conserver la première lettre
        val premiereLettre = dernier.mot.first()

        val tirets = "-".repeat(
            dernier.mot.length - 1
        )

        texte = texte.replaceRange(
            dernier.debut,
            dernier.fin,
            premiereLettre + tirets
        )

        // Remettre le mot dans la pile
        pile.addLast(dernier)

        // Il n'y a plus de mot récemment réaffiché
        dernierMotAffiche = null

        afficherTexte()

        // Sauvegarde automatique
        sauverEtat()
    }

    // =========================================================
    // RESET
    // =========================================================

    private fun resetTexte() {

        // Relire le fichier original
        texte = assets.open("Dormeur.txt")
            .bufferedReader()
            .use { it.readText() }

        // Vider la pile
        pile.clear()

        // Oublier le dernier mot réaffiché
        dernierMotAffiche = null

        afficherTexte()

        // Sauvegarder l'état initial
        sauverEtat()
    }

    // =========================================================
    // SAUVEGARDER L'ÉTAT
    // =========================================================

    private fun sauverEtat() {

        // -----------------------------------------------------
        // Sauvegarder le texte
        // -----------------------------------------------------

        val editor = preferences.edit()

        editor.putString(
            "texte",
            texte
        )

        // -----------------------------------------------------
        // Sauvegarder la pile
        // -----------------------------------------------------

        val tableauPile = JSONArray()

        for (mot in pile) {

            val objet = JSONObject()

            objet.put("debut", mot.debut)
            objet.put("fin", mot.fin)
            objet.put("mot", mot.mot)

            tableauPile.put(objet)
        }

        editor.putString(
            "pile",
            tableauPile.toString()
        )

        // -----------------------------------------------------
        // Sauvegarder le dernier mot réaffiché
        // -----------------------------------------------------

        if (dernierMotAffiche != null) {

            val objet = JSONObject()

            objet.put(
                "debut",
                dernierMotAffiche!!.debut
            )

            objet.put(
                "fin",
                dernierMotAffiche!!.fin
            )

            objet.put(
                "mot",
                dernierMotAffiche!!.mot
            )

            editor.putString(
                "dernierMotAffiche",
                objet.toString()
            )

        } else {

            editor.remove("dernierMotAffiche")
        }

        editor.apply()
    }

    // =========================================================
    // RESTAURER L'ÉTAT
    // =========================================================

    private fun restaurerEtat() {

        val texteSauve = preferences.getString(
            "texte",
            null
        )

        // -----------------------------------------------------
        // S'il n'y a aucune sauvegarde :
        // charger Dormeur.txt
        // -----------------------------------------------------

        if (texteSauve == null) {

            texte = assets.open("Dormeur.txt")
                .bufferedReader()
                .use { it.readText() }

            pile.clear()
            dernierMotAffiche = null

            afficherTexte()

            return
        }

        // -----------------------------------------------------
        // Restaurer le texte
        // -----------------------------------------------------

        texte = texteSauve

        // -----------------------------------------------------
        // Restaurer la pile
        // -----------------------------------------------------

        pile.clear()

        val chainePile = preferences.getString(
            "pile",
            null
        )

        if (chainePile != null) {

            val tableauPile = JSONArray(
                chainePile
            )

            for (i in 0 until tableauPile.length()) {

                val objet = tableauPile.getJSONObject(i)

                pile.addLast(
                    MotMasque(
                        debut = objet.getInt("debut"),
                        fin = objet.getInt("fin"),
                        mot = objet.getString("mot")
                    )
                )
            }
        }

        // -----------------------------------------------------
        // Restaurer le dernier mot réaffiché
        // -----------------------------------------------------

        dernierMotAffiche = null

        val chaineDernier = preferences.getString(
            "dernierMotAffiche",
            null
        )

        if (chaineDernier != null) {

            val objet = JSONObject(
                chaineDernier
            )

            dernierMotAffiche = MotMasque(
                debut = objet.getInt("debut"),
                fin = objet.getInt("fin"),
                mot = objet.getString("mot")
            )
        }

        // Afficher le texte restauré
        afficherTexte()
    }
}
