package com.liferay.support.tools.journal;

import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.support.tools.common.DummyGenerator;

import java.util.Calendar;
import java.util.Date;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = JournalStructureBaseDummyGenerator.class)
public class JournalStructureBaseDummyGenerator extends DummyGenerator<JournalContext> {

	@Override
	protected JournalContext getContext(ActionRequest request) throws Exception {
		return new JournalContext(request);
	}

	@Override
	protected void exec(ActionRequest request, JournalContext paramContext) throws Exception {

	}

	/**
	 * Update Article (Never expired, Never reviewed)
	 * 
	 * @param createdArticle
	 * @param paramContext
	 * @return
	 * @throws PortalException
	 */
	protected JournalArticle updateArticle(JournalArticle createdArticle, JournalContext paramContext)
			throws PortalException {

		User user = _userLocalService.getUser(createdArticle.getUserId());

		Date displayDate = createdArticle.getDisplayDate();

		int displayDateMonth = 0;
		int displayDateDay = 0;
		int displayDateYear = 0;
		int displayDateHour = 0;
		int displayDateMinute = 0;

		if (displayDate != null) {
			Calendar displayCal = CalendarFactoryUtil.getCalendar(
					user.getTimeZone());

			displayCal.setTime(displayDate);

			displayDateMonth = displayCal.get(Calendar.MONTH);
			displayDateDay = displayCal.get(Calendar.DATE);
			displayDateYear = displayCal.get(Calendar.YEAR);
			displayDateHour = displayCal.get(Calendar.HOUR);
			displayDateMinute = displayCal.get(Calendar.MINUTE);

			if (displayCal.get(Calendar.AM_PM) == Calendar.PM) {
				displayDateHour += 12;
			}
		}
		
		Date expirationDate = createdArticle.getExpirationDate();
		
		int expirationDateMonth = 0;
		int expirationDateDay = 0;
		int expirationDateYear = 0;
		int expirationDateHour = 0;
		int expirationDateMinute = 0;

		if (!paramContext.isNeverExpire()) {
			Calendar expirationCal = CalendarFactoryUtil.getCalendar(
					user.getTimeZone());

			if (expirationDate == null) {
				expirationCal.setTime(new Date());
				expirationCal.add(Calendar.MONTH, 1);
			} else {
				expirationCal.setTime(expirationDate);
			}

			expirationDateMonth = expirationCal.get(Calendar.MONTH);
			expirationDateDay = expirationCal.get(Calendar.DATE);
			expirationDateYear = expirationCal.get(Calendar.YEAR);
			expirationDateHour = expirationCal.get(Calendar.HOUR);
			expirationDateMinute = expirationCal.get(Calendar.MINUTE);

			if (expirationCal.get(Calendar.AM_PM) == Calendar.PM) {
				expirationDateHour += 12;
			}
		}

		Date reviewDate = createdArticle.getReviewDate();
		
		int reviewDateMonth = 0;
		int reviewDateDay = 0;
		int reviewDateYear = 0;
		int reviewDateHour = 0;
		int reviewDateMinute = 0;

		if (!paramContext.isNeverReview()) {
			Calendar reviewCal = CalendarFactoryUtil.getCalendar(
					user.getTimeZone());

			if (reviewDate == null) {
				reviewCal.setTime(new Date());
				reviewCal.add(Calendar.MONTH, 1);
			} else {
				reviewCal.setTime(reviewDate);
			}			

			reviewDateMonth = reviewCal.get(Calendar.MONTH);
			reviewDateDay = reviewCal.get(Calendar.DATE);
			reviewDateYear = reviewCal.get(Calendar.YEAR);
			reviewDateHour = reviewCal.get(Calendar.HOUR);
			reviewDateMinute = reviewCal.get(Calendar.MINUTE);

			if (reviewCal.get(Calendar.AM_PM) == Calendar.PM) {
				reviewDateHour += 12;
			}
		}	
		
		// Update contents modified date is validated in updateArticle,
		// So update the date to the latest here for serviceContext
		ServiceContext serviceContext = paramContext.getServiceContext();
		serviceContext.setFormDate(new Date());
		
		return _journalArticleLocalService.updateArticle(
				createdArticle.getUserId(),
				createdArticle.getGroupId(),
				createdArticle.getFolderId(),
				createdArticle.getArticleId(),
				createdArticle.getVersion(),
				createdArticle.getTitleMap(),
				createdArticle.getDescriptionMap(),
				createdArticle.getContent(),
				createdArticle.getDDMStructureKey(),
				createdArticle.getDDMTemplateKey(),
				createdArticle.getLayoutUuid(),
				displayDateMonth,
				displayDateDay,
				displayDateYear,
				displayDateHour,
				displayDateMinute,
				expirationDateMonth,
				expirationDateDay,
				expirationDateYear,
				expirationDateHour,
				expirationDateMinute,
				paramContext.isNeverExpire(),
				reviewDateMonth,
				reviewDateDay,
				reviewDateYear,
				reviewDateHour,
				reviewDateMinute,
				paramContext.isNeverReview(),
				createdArticle.isIndexable(),
				createdArticle.isSmallImage(),
				createdArticle.getSmallImageURL(),
				null,
				null,
				null,
				serviceContext);
	}

	@Reference(unbind = "-")
	protected void setJournalArticleLocalService(
			JournalArticleLocalService journalArticleLocalService) {

		_journalArticleLocalService = journalArticleLocalService;
	}
	
	@Reference(unbind = "-")
	protected void setUserLocalService(
			UserLocalService userLocalService) {

		_userLocalService = userLocalService;
	}

	private JournalArticleLocalService _journalArticleLocalService;
	private UserLocalService _userLocalService;

}
