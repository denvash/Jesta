package com.jesta.gui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.jesta.R
import com.jesta.data.*
import com.jesta.utils.db.ChatManager
import com.jesta.data.chat.ChatRoom
import com.jesta.gui.activities.MainActivity
import com.jesta.utils.db.SysManager
import com.squareup.picasso.Picasso
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.fragment_card_preview.view.*
import kotlinx.android.synthetic.main.jesta_card_preview.view.*
import kotlinx.android.synthetic.main.jesta_main_activity.*
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*

class CardReviewFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(mission: Mission) = CardReviewFragment().apply {
            arguments = Bundle().apply {
                putParcelable(BUNDLE_MISSION, mission)
            }
        }
    }

    private val sysManager = MainActivity.instance.sysManager
    private lateinit var mission: Mission

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        arguments?.getParcelable<Mission>(BUNDLE_MISSION)?.let {
            mission = it
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_card_preview, container, false)

        val posterAvatar = sysManager.getUserByID(mission.posterID).photoUrl
        Picasso.get().load(posterAvatar).noFade().into(view.jesta_preview_avatar_icon)

        view.jesta_preview_title.text = mission.title
        view.jesta_preview_difficulty.text = mission.difficulty

        Picasso.get()
            .load(mission.imageUrl)
            .resize(6000, 2000)
            .onlyScaleDown()
            .placeholder(R.drawable.jesta_empty_default)
            .into(view.jesta_card_preview_mission_image)

        view.jesta_preview_description.text = mission.description
        view.jesta_preview_payment.text = mission.payment.toString()
        view.jesta_preview_crew.text = mission.numOfPeople.toString()
        view.jesta_preview_duration.text = mission.duration.toString()
        view.jesta_preview_location.text = mission.location
        view.jesta_preview_diamonds.text = mission.diamonds.toString()

        view.jesta_card_preview_tags_layout.tagTypeface =
                ResourcesCompat.getFont(MainActivity.instance, R.font.montserrat_light_italic)
        view.jesta_card_preview_tags_layout.tags = mission.tags

        view.jesta_preview_button_back.setOnClickListener {
            MainActivity.instance.fragNavController.popFragment()
        }

        view.jesta_preview_accept_button.setOnClickListener {

            sysManager.getUserRelations(sysManager.currentUserFromDB.id).addOnCompleteListener { getRelationsTask ->
                val result: List<Relation> = (getRelationsTask.result as List<*>).filterIsInstance<Relation>()

                val currMissionRelation = result.find { relation -> relation.missionID == mission.id }

                if (mission.posterID == sysManager.currentUserFromDB.id) {
                    Alerter.create(MainActivity.instance)
                        .setTitle("Don't be Silly! \uD83D\uDE1C")
                        .setText("You can't do your own Jesta!")
                        .setBackgroundColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_jesta_diamond_normal)
                        .show()
                    return@addOnCompleteListener
                } else if (currMissionRelation != null && currMissionRelation.doerID == sysManager.currentUserFromDB.id) {
                    Alerter.create(MainActivity.instance)
                        .setTitle("You are a Doer already! \uD83D\uDCAA")
                        .setText("Check out the Status tab!")
                        .setBackgroundColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_jesta_diamond_normal)
                        .show()
                    return@addOnCompleteListener
                } else {
                    Alerter.create(MainActivity.instance)
                        .setTitle("You Offered to Do a Jesta!\uD83E\uDD19")
                        .setText(
                            "Great Job! Check out the Status tab! " +
                                    "The poster has been notified!"
                        )
                        .setBackgroundColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_jesta_diamond_normal)
                        .show()

                    val relation = Relation(
                        id = UUID.randomUUID().toString(),
                        doerID = sysManager.currentUserFromDB.id,
                        posterID = mission.posterID,
                        missionID = mission.id,
                        status = RELATION_STATUS_INIT
                    )
                    sysManager.setRelationOnDB(relation)
                    sysManager.askTodoJestaForUser(mission)

                    // subscribe doer to chat room
                    val doer = sysManager.currentUserFromDB
                    val poster = sysManager.getUserByID(mission.posterID)
                    val chatRoom = ChatRoom(doer, poster, mission)
                    val chatManager = ChatManager(MainActivity.instance)
                    chatManager.subscribeToChatRoom(chatRoom)
                }
                MainActivity.instance.fragNavController.popFragment()
                MainActivity.instance.fragNavController.switchTab(INDEX_STATUS)
                val statusFragment = MainActivity.instance.fragNavController.currentFrag as StatusFragment

                sysManager.createDBTask(SysManager.DBTask.RELOAD_JESTAS)
                    .addOnCompleteListener {

                        sysManager.statusList
                            .addOnCompleteListener { refreshRelationsTask ->

                                @Suppress("LABEL_NAME_CLASH")
                                if (!refreshRelationsTask.isSuccessful) {
                                    MainActivity.instance.alertError(refreshRelationsTask.exception!!.message)
                                    return@addOnCompleteListener
                                }
                                statusFragment.updateStatus(statusFragment.fragView , refreshRelationsTask)
                            }
                    }
                MainActivity.instance.jesta_bottom_navigation.selectedItemId = R.id.nav_status
            }
        }

        OverScrollDecoratorHelper.setUpOverScroll(view.jesta_preview_nested_scroll_view)
        return view
    }

}