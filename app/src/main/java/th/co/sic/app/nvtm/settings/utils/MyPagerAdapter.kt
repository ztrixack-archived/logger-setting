package th.co.sic.app.nvtm.settings.utils

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import th.co.sic.app.nvtm.settings.fragments.ConfigFragment
import th.co.sic.app.nvtm.settings.R

class MyPagerAdapter(fm: FragmentManager?, context: Context) : FragmentPagerAdapter(fm) {
    private val tabTitles: Array<String> = arrayOf(context.resources.getString(R.string.action_config))
    override fun getItem(index: Int): Fragment? {
        return if (Position.ToEnum(index) == Position.CONFIG) {
            ConfigFragment()
        } else null
    }

    override fun getCount(): Int {
        return tabTitles.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }

    enum class Position(private val position: Int) {
        CONFIG(0);

        fun Compare(position: Int): Boolean {
            return this.position == position
        }

        companion object {
            fun ToEnum(position: Int): Position {
                val ids = values()
                for (x in ids) {
                    if (x.Compare(position)) {
                        return x
                    }
                }
                return CONFIG
            }

            fun ToInt(position: Position): Int {
                return position.position
            }
        }

    }

}