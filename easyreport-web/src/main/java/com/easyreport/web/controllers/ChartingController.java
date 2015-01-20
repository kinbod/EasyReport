package com.easyreport.web.controllers;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONObject;
import com.easyreport.engine.data.ReportDataSet;
import com.easyreport.engine.exception.NotFoundLayoutColumnException;
import com.easyreport.engine.exception.SQLQueryException;
import com.easyreport.engine.exception.TemplatePraseException;
import com.easyreport.exception.QueryParamsException;
import com.easyreport.po.ReportingPo;
import com.easyreport.service.ReportingChartService;
import com.easyreport.service.ReportingGenerationService;
import com.easyreport.service.ReportingService;
import com.easyreport.view.EasyUIQueryFormView;
import com.easyreport.viewmodel.HtmlFormElement;

/**
 * 报表图表生成控制器
 */
@Controller
@RequestMapping(value = "/report/chart")
public class ChartingController extends AbstractController {
	@Resource
	private ReportingService reportingService;
	@Resource
	private ReportingGenerationService generationService;
	@Resource
	private ReportingChartService reportChartService;

	@RequestMapping(value = { "/{uid}" })
	public ModelAndView index(@PathVariable String uid, HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("report/chart");

		try {
			ReportingPo po = reportingService.getByUid(uid);
			Map<String, Object> buildinParams = generationService.getBuildInParameters(request.getParameterMap(), po.getDataRange());
			List<HtmlFormElement> formElements = generationService.getFormElements(po, buildinParams, 0);
			EasyUIQueryFormView formView = new EasyUIQueryFormView();
			modelAndView.addObject("uid", uid);
			modelAndView.addObject("id", po.getId());
			modelAndView.addObject("name", po.getName());
			modelAndView.addObject("comment", po.getComment().trim());
			modelAndView.addObject("formHtmlText", formView.getFormHtmlText(formElements));
		} catch (QueryParamsException | TemplatePraseException ex) {
			modelAndView.addObject("message", ex.getMessage());
			this.logException("查询参数生成失败", ex);
		} catch (Exception ex) {
			modelAndView.addObject("message", "查询参数生成失败！请联系管理员.");
			this.logException("查询参数生成失败", ex);
		}

		return modelAndView;
	}

	@RequestMapping(value = "/getData", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject getData(String uid, Integer id, HttpServletRequest request) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("dimColumnSelects", "");
		jsonObject.put("dimColumns", null);
		jsonObject.put("statColumns", null);
		jsonObject.put("dataRows", null);
		jsonObject.put("msg", "");

		if (id == null) {
			return jsonObject;
		}

		try {
			ReportingPo po = reportingService.getByUid(uid);
			Map<String, Object> formParameters = generationService.getFormParameters(request.getParameterMap(), po.getDataRange());
			ReportDataSet reportData = generationService.getReportDataSet(po, formParameters);
			jsonObject.put("dimColumnSelects", reportChartService.buildDimColumnsHtml(reportData));
			jsonObject.put("dimColumns", reportChartService.getDimColumns(reportData));
			jsonObject.put("statColumns", reportChartService.getStatColumns(reportData));
			jsonObject.put("dataRows", reportChartService.getDataRows(reportData));
		} catch (QueryParamsException | NotFoundLayoutColumnException | SQLQueryException | TemplatePraseException ex) {
			jsonObject.put("msg", ex.getMessage());
			this.logException("报表生成失败", ex);
		} catch (Exception ex) {
			jsonObject.put("msg", "报表生成失败！请联系管理员。");
			this.logException("报表生成失败", ex);
		}

		return jsonObject;
	}
}