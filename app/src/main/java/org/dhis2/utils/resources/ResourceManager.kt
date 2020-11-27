package org.dhis2.utils.resources

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import org.dhis2.R
import org.dhis2.utils.ColorUtils

class ResourceManager(val context: Context) {

    fun getObjectStyleDrawableResource(icon: String?, @DrawableRes defaultResource: Int): Int {
        return icon?.let {
            val iconName = if (icon.startsWith("ic_")) icon else "ic_$icon"
            var iconResource =
                context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (iconResource != 0 && iconResource != -1 && drawableExists(iconResource)
            ) {
                iconResource
            } else {
                R.drawable.ic_default_icon
            }
        } ?: defaultResource
    }

    private fun drawableExists(iconResource: Int): Boolean {
        return try {
            ContextCompat.getDrawable(context, iconResource)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getColorFrom(hexColor: String?): Int {
        return hexColor?.let {
            ColorUtils.parseColor(it)
        } ?: -1
    }

    fun defaultEventLabel(): String = context.getString(R.string.events)
    fun defaultDataSetLabel(): String = context.getString(R.string.data_sets)
    fun defaultTeiLabel(): String = context.getString(R.string.tei)
    fun jiraIssueSentMessage(): String = context.getString(R.string.jira_issue_sent)
    fun jiraIssueSentErrorMessage(): String = context.getString(R.string.jira_issue_sent_error)
    fun defaultWorkingListLabel(): String = context.getString(R.string.working_list_default_label)
    fun todayLabel(): String = context.getString(R.string.filter_period_today)
    fun yesterdayLabel(): String = context.getString(R.string.filter_period_yesterday)
    fun lastNDays(days: Int): String =
        context.getString(R.string.filter_period_last_n_days).format(days)

    fun thisMonthLabel(): String = context.getString(R.string.filter_period_this_month)
    fun lastMonthLabel(): String = context.getString(R.string.filter_period_last_month)
    fun thisBiMonth(): String = "This bimonth"
    fun lastBiMonth(): String = "Last bimonth"
    fun thisQuarter(): String = "This quarter"
    fun lastQuarter(): String = "Last quarter"
    fun thisSixMonth(): String = "This six months"
    fun lastSixMonth(): String = "Last six months"
    fun lastNYears(years: Int): String = "Last %d years".format(years)
    fun lastNMonths(months: Int): String = "Last %d months".format(months)
    fun lastNWeeks(weeks: Int): String = "Last %d weeks".format(weeks)
    fun weeksThisYear(): String = "Weeks this year"
    fun monthsThisYear(): String = "Months this year"
    fun bimonthsThisYear(): String = "Bimonths this year"
    fun quartersThisYear(): String = "Quarters this year"
    fun thisYear(): String = context.getString(R.string.filter_period_this_year)
    fun monthsLastYear(): String = "Months last year"
    fun quartersLastYear(): String = "Quarters las years"
    fun lastYear(): String = "Last year"
    fun thisWeek(): String = "This week"
    fun lastWeek(): String = "Last week"
    fun thisBiWeek(): String = "This biweek"
    fun lastBiWeek(): String = "Last biweek"
    fun lastNBimonths(bimonths: Int): String = "Last %d bimonths".format(bimonths)
    fun lastNQuarters(quarters: Int): String = "Last %d quarters".format(quarters)
    fun lastNSixMonths(months: Int): String = "Last %d months".format(months)
    fun thisFinancialYear(): String = "This financial year"
    fun lastFinancialYear(): String = "Last financial year"
    fun lastNFinancialYears(financialYears: Int): String =
        "Last %d financial years".format(financialYears)

    fun lastNBiWeeks(biweeks: Int) = "Last %d biweeks".format(biweeks)

    fun span():String = "%s - %s"

}
