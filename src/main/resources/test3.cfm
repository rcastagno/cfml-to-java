<cfquery name="news">
    SELECT id,title,story
    FROM news
    WHERE id = <cfqueryparam value="#url.id#" cfsqltype="cf_sql_integer">
    AND id2 = <cfqueryparam value="#url.id2#" cfsqltype="cf_sql_integer">
    AND id3 = #url.id3#
</cfquery>