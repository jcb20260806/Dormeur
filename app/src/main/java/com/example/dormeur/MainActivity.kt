
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

class MainActivity : AppCompatActivity() {

    private lateinit var txtDormeur: TextView
    private lateinit var btnMasquer: Button
    private lateinit var btnAfficher: Button
    private lateinit var btnReset: Button
    private var dernierMotAffiche: MotMasque? = null
    private lateinit var btnOptions: Button

    // Texte actuellement affiché
    private var texte = ""

    // Élément de la pile
    data class MotMasque(
        val debut: Int,
        val fin: Int,
        val mot: String
    )

    // La pile des mots masqués
    private val pile = ArrayDeque<MotMasque>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        txtDormeur = findViewById(R.id.txtDormeur)
        btnMasquer = findViewById(R.id.btnMasquer)
        btnAfficher = findViewById(R.id.btnAfficher)
        btnReset = findViewById(R.id.btnReset)
        btnOptions = findViewById(R.id.btnOptions)

        btnOptions.setOnClickListener {
            startActivity(
                Intent(this, OptionsActivity::class.java)
            )
        }

        // Lecture du fichier Dormeur.txt
        texte = assets.open("Dormeur.txt")
            .bufferedReader()
            .use { it.readText() }

        // Affichage initial
        afficherTexte()

        // Bouton MASQUER
        btnMasquer.setOnClickListener {
            masquerMotAleatoire()
        }

        // Bouton AFFICHER
        btnAfficher.setOnClickListener {
            afficherDernierMot()

        }
        btnReset.setOnClickListener {
            resetTexte()
        }

    }

    // ---------------------------------------------------------
    // Masque un mot choisi au hasard
    // ---------------------------------------------------------
    private fun afficherTexte() {

        val spannable = SpannableString(texte)

        var debutLigne = 0

        for (i in texte.indices) {

            // Fin de ligne
            if (texte[i] == '\n') {

                colorerPremiereLettre(
                    spannable,
                    debutLigne,
                    i
                )

                debutLigne = i + 1
            }
        }

        // Traiter également la dernière ligne
        colorerPremiereLettre(
            spannable,
            debutLigne,
            texte.length
        )

        txtDormeur.text = spannable
    }
    private fun colorerPremiereLettre(
        spannable: SpannableString,
        debut: Int,
        fin: Int
    ) {

        // Chercher le premier caractère non blanc
        for (i in debut until fin) {

            if (!texte[i].isWhitespace()) {

                // Colorer ce caractère en rouge
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
    private fun masquerMotAleatoire() {

        val regex = Regex("\\b[\\p{L}\\p{N}]+\\b")
        val mots = regex.findAll(texte).toList()

        if (mots.isEmpty()) {
            return
        }

        // Choisir un mot au hasard
        val mot = mots[Random.nextInt(mots.size)]

        // Sauvegarder le mot dans la pile
        pile.addLast(
            MotMasque(
                debut = mot.range.first,
                fin = mot.range.last + 1,
                mot = mot.value
            )
        )

        // Créer les tirets
        // Conserver la première lettre
        val premiereLettre = mot.value.first()

// Remplacer le reste du mot par des tirets
        val tirets = "-".repeat(mot.value.length - 1)

        texte = texte.replaceRange(
            mot.range,
            premiereLettre + tirets
        )

        // Afficher le texte
        afficherTexte()
    }

    // ---------------------------------------------------------
    // Réaffiche le dernier mot masqué
    // ---------------------------------------------------------

    private fun afficherDernierMot() {

        // Vérifier si la pile est vide
        if (pile.isEmpty()) {
            return
        }

        // LIFO :
        // on récupère le dernier mot ajouté
        val dernier = pile.removeLast()

        // Remplacer les tirets par le mot original
        texte = texte.replaceRange(
            dernier.debut,
            dernier.fin,
            dernier.mot
        )

        // Afficher le texte
        afficherTexte()
    }
    private fun resetTexte() {

        // Relire le fichier original
        texte = assets.open("Dormeur.txt")
            .bufferedReader()
            .use { it.readText() }

        // Vider la pile
        pile.clear()

        // Réafficher le texte original
        afficherTexte()
    }
}

