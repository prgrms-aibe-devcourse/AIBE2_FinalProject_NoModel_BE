package com.example.nomodel.model.domain.repository;

import com.example.nomodel.model.domain.model.document.AIModelDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * AIModel Elasticsearch 리포지토리
 * AI 모델 검색을 위한 다양한 쿼리 메서드 제공
 */
@Repository
public interface AIModelSearchRepository extends ElasticsearchRepository<AIModelDocument, String> {

    /**
     * 공개된 모델만 검색
     */
    Page<AIModelDocument> findByIsPublic(Boolean isPublic, Pageable pageable);

    /**
     * 소유자별 검색
     */
    Page<AIModelDocument> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 모델 ID로 검색
     */
    Page<AIModelDocument> findByModelId(Long modelId, Pageable pageable);

    /**
     * 소유 타입별 검색 (USER, ADMIN)
     */
    Page<AIModelDocument> findByOwnTypeAndIsPublic(String ownType, Boolean isPublic, Pageable pageable);

    /**
     * 모델명으로 검색 (한글 분석기 적용)
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"modelName\": \"?0\"}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> searchByModelName(String keyword, Pageable pageable);

    /**
     * 모델명과 프롬프트에서 통합 검색 (정확도 우선)
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "bool": {
                  "should": [
                    {
                      "match_phrase": {
                        "modelName": {
                          "query": "?0",
                          "boost": 10
                        }
                      }
                    },
                    {
                      "match": {
                        "modelName": {
                          "query": "?0",
                          "boost": 5,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "prefix": {
                        "modelName.keyword": {
                          "value": "?0",
                          "boost": 4
                        }
                      }
                    },
                    {
                      "match": {
                        "prompt": {
                          "query": "?0",
                          "boost": 2,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "terms": {
                        "tags": ["?0"],
                        "boost": 3
                      }
                    }
                  ],
                  "minimum_should_match": 1
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "isPublic": true
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchByModelNameAndPrompt(String keyword, Pageable pageable);

    /**
     * 태그로 검색
     */
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"tags\": [\"?0\"]}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> searchByTag(String tag, Pageable pageable);

    /**
     * 프롬프트로 검색
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"prompt\": \"?0\"}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> searchByPrompt(String keyword, Pageable pageable);

    /**
     * 가격 범위로 검색
     */
    @Query("{\"bool\": {\"must\": [{\"range\": {\"price\": {\"gte\": ?0, \"lte\": ?1}}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 무료 모델 검색
     */
    @Query("{\"bool\": {\"should\": [{\"bool\": {\"must_not\": {\"exists\": {\"field\": \"price\"}}}}, {\"term\": {\"price\": 0}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> findFreeModels(Pageable pageable);

    /**
     * 인기 모델 검색 (사용량 기준)
     */
    @Query("{\"bool\": {\"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> findPopularModels(Pageable pageable);

    /**
     * 최신 모델 검색
     */
    @Query("{\"bool\": {\"filter\": [{\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> findRecentModels(Pageable pageable);

    /**
     * 평점 높은 모델 검색
     */
    @Query("{\"bool\": {\"must\": [{\"range\": {\"rating\": {\"gte\": ?0}}}], \"filter\": [{\"term\": {\"isPublic\": true}}, {\"range\": {\"reviewCount\": {\"gte\": 1}}}]}}")
    Page<AIModelDocument> findHighRatedModels(Double minRating, Pageable pageable);

    /**
     * 관리자 추천 모델 검색
     */
    @Query("{\"bool\": {\"filter\": [{\"term\": {\"ownType\": \"ADMIN\"}}, {\"term\": {\"isPublic\": true}}]}}")
    Page<AIModelDocument> findRecommendedModels(Pageable pageable);


    /**
     * 유사 모델 검색 (More Like This)
     */
    @Query("{\"more_like_this\": {\"fields\": [\"modelName\", \"prompt\", \"tags\"], \"like\": [{\"_index\": \"ai-models\", \"_id\": \"?0\"}], \"min_term_freq\": 1, \"max_query_terms\": 12}}")
    Page<AIModelDocument> findSimilarModels(String modelId, Pageable pageable);

    /**
     * 복합 필터 검색 (태그와 가격 범위)
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"modelName^3\", \"prompt^2\"]}}], \"filter\": [{\"term\": {\"isPublic\": true}}, {\"terms\": {\"tags\": [\"?1\"]}}, {\"range\": {\"price\": {\"gte\": ?2, \"lte\": ?3}}}]}}")
    Page<AIModelDocument> searchWithMultipleFilters(String keyword, String tag, 
                                                   BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 특정 사용자가 접근 가능한 모델 검색 (본인 모델 + 공개 모델)
     */
    @Query("{\"bool\": {\"should\": [{\"bool\": {\"must\": [{\"term\": {\"ownerId\": ?0}}]}}, {\"bool\": {\"must\": [{\"term\": {\"isPublic\": true}}]}}]}}")
    Page<AIModelDocument> findAccessibleModels(Long userId, Pageable pageable);

    /**
     * 키워드로 접근 가능한 모델 검색
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"modelName^3\", \"prompt^2\", \"tags\"]}}], \"should\": [{\"bool\": {\"must\": [{\"term\": {\"ownerId\": ?1}}]}}, {\"bool\": {\"must\": [{\"term\": {\"isPublic\": true}}]}}]}}")
    Page<AIModelDocument> searchAccessibleModels(String keyword, Long userId, Pageable pageable);

    /**
     * 하이라이트 기능을 포함한 검색
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"modelName^2\", \"prompt\"]}}], \"filter\": [{\"term\": {\"isPublic\": true}}]}, \"highlight\": {\"fields\": {\"modelName\": {}, \"prompt\": {}}}}")
    Page<AIModelDocument> searchWithHighlight(String keyword, Pageable pageable);

    /**
     * 관리자 모델에서 키워드 검색 (ADMIN 타입 + 공개 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "bool": {
                  "should": [
                    {
                      "match_phrase": {
                        "modelName": {
                          "query": "?0",
                          "boost": 10
                        }
                      }
                    },
                    {
                      "match": {
                        "modelName": {
                          "query": "?0",
                          "boost": 5,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "prefix": {
                        "modelName.keyword": {
                          "value": "?0",
                          "boost": 4
                        }
                      }
                    },
                    {
                      "match": {
                        "prompt": {
                          "query": "?0",
                          "boost": 2,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "terms": {
                        "tags": ["?0"],
                        "boost": 3
                      }
                    }
                  ],
                  "minimum_should_match": 1
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownType": "ADMIN"
                }
              },
              {
                "term": {
                  "isPublic": true
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchInAdminModels(String keyword, Pageable pageable);

    /**
     * 사용자 모델에서 키워드 검색 (특정 사용자 소유 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "bool": {
                  "should": [
                    {
                      "match_phrase": {
                        "modelName": {
                          "query": "?0",
                          "boost": 10
                        }
                      }
                    },
                    {
                      "match": {
                        "modelName": {
                          "query": "?0",
                          "boost": 5,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "prefix": {
                        "modelName.keyword": {
                          "value": "?0",
                          "boost": 4
                        }
                      }
                    },
                    {
                      "match": {
                        "prompt": {
                          "query": "?0",
                          "boost": 2,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "terms": {
                        "tags": ["?0"],
                        "boost": 3
                      }
                    }
                  ],
                  "minimum_should_match": 1
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownerId": ?1
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchInUserModels(String keyword, Long userId, Pageable pageable);

    /**
     * 키워드와 무료 필터링 조합 검색 (공개 모델 + 키워드 + 무료)
     */
    @Query("""
        {
          "bool": {
            "must": [
              {
                "bool": {
                  "should": [
                    {
                      "match_phrase": {
                        "modelName": {
                          "query": "?0",
                          "boost": 10
                        }
                      }
                    },
                    {
                      "match": {
                        "modelName": {
                          "query": "?0",
                          "boost": 5,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "prefix": {
                        "modelName.keyword": {
                          "value": "?0",
                          "boost": 4
                        }
                      }
                    },
                    {
                      "match": {
                        "prompt": {
                          "query": "?0",
                          "boost": 2,
                          "fuzziness": "AUTO"
                        }
                      }
                    },
                    {
                      "terms": {
                        "tags": ["?0"],
                        "boost": 3
                      }
                    }
                  ],
                  "minimum_should_match": 1
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "bool": {
                  "should": [
                    {
                      "bool": {
                        "must_not": {
                          "exists": {
                            "field": "price"
                          }
                        }
                      }
                    },
                    {
                      "term": {
                        "price": 0
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchFreeModelsWithKeyword(String keyword, Pageable pageable);

    /**
     * 무료 관리자 모델 조회 (ADMIN 타입 + 공개 + 무료)
     */
    @Query("""
        {
          "bool": {
            "filter": [
              {
                "term": {
                  "ownType": "ADMIN"
                }
              },
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "bool": {
                  "should": [
                    {
                      "bool": {
                        "must_not": {
                          "exists": {
                            "field": "price"
                          }
                        }
                      }
                    },
                    {
                      "term": {
                        "price": 0
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchFreeAdminModels(Pageable pageable);

    /**
     * 무료 관리자 모델에서 키워드 검색 (ADMIN 타입 + 공개 + 무료 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "modelName": {
                    "query": "?0",
                    "boost": 10
                  }
                }
              },
              {
                "match": {
                  "modelName": {
                    "query": "?0",
                    "boost": 5,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "modelName.keyword": {
                    "value": "*?0*",
                    "boost": 3,
                    "case_insensitive": true
                  }
                }
              },
              {
                "match": {
                  "prompt": {
                    "query": "?0",
                    "boost": 2,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "tags.keyword": {
                    "value": "*?0*",
                    "boost": 1.5,
                    "case_insensitive": true
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownType": "ADMIN"
                }
              },
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "bool": {
                  "should": [
                    {
                      "bool": {
                        "must_not": {
                          "exists": {
                            "field": "price"
                          }
                        }
                      }
                    },
                    {
                      "term": {
                        "price": 0
                      }
                    }
                  ]
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<AIModelDocument> searchFreeAdminModelsWithKeyword(String keyword, Pageable pageable);

    /**
     * 무료 사용자 모델 조회 (특정 사용자 소유 + 무료)
     */
    @Query("""
        {
          "bool": {
            "filter": [
              {
                "term": {
                  "ownerId": ?0
                }
              },
              {
                "bool": {
                  "should": [
                    {
                      "bool": {
                        "must_not": {
                          "exists": {
                            "field": "price"
                          }
                        }
                      }
                    },
                    {
                      "term": {
                        "price": 0
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchFreeUserModels(Long userId, Pageable pageable);

    /**
     * 무료 사용자 모델에서 키워드 검색 (특정 사용자 소유 + 무료 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "modelName": {
                    "query": "?0",
                    "boost": 10
                  }
                }
              },
              {
                "match": {
                  "modelName": {
                    "query": "?0",
                    "boost": 5,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "modelName.keyword": {
                    "value": "*?0*",
                    "boost": 3,
                    "case_insensitive": true
                  }
                }
              },
              {
                "match": {
                  "prompt": {
                    "query": "?0",
                    "boost": 2,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "tags.keyword": {
                    "value": "*?0*",
                    "boost": 1.5,
                    "case_insensitive": true
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownerId": ?1
                }
              },
              {
                "bool": {
                  "should": [
                    {
                      "bool": {
                        "must_not": {
                          "exists": {
                            "field": "price"
                          }
                        }
                      }
                    },
                    {
                      "term": {
                        "price": 0
                      }
                    }
                  ]
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<AIModelDocument> searchFreeUserModelsWithKeyword(String keyword, Long userId, Pageable pageable);

    /**
     * 유료 모델 검색 (공개 모델 + 유료)
     */
    @Query("""
        {
          "bool": {
            "filter": [
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> findPaidModels(Pageable pageable);

    /**
     * 키워드와 유료 필터링 조합 검색 (공개 모델 + 키워드 + 유료)
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "modelName": {
                    "query": "?0",
                    "boost": 10
                  }
                }
              },
              {
                "match": {
                  "modelName": {
                    "query": "?0",
                    "boost": 5,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "modelName.keyword": {
                    "value": "*?0*",
                    "boost": 3,
                    "case_insensitive": true
                  }
                }
              },
              {
                "match": {
                  "prompt": {
                    "query": "?0",
                    "boost": 2,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "tags.keyword": {
                    "value": "*?0*",
                    "boost": 1.5,
                    "case_insensitive": true
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<AIModelDocument> searchPaidModelsWithKeyword(String keyword, Pageable pageable);

    /**
     * 유료 관리자 모델 조회 (ADMIN 타입 + 공개 + 유료)
     */
    @Query("""
        {
          "bool": {
            "filter": [
              {
                "term": {
                  "ownType": "ADMIN"
                }
              },
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchPaidAdminModels(Pageable pageable);

    /**
     * 유료 관리자 모델에서 키워드 검색 (ADMIN 타입 + 공개 + 유료 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "modelName": {
                    "query": "?0",
                    "boost": 10
                  }
                }
              },
              {
                "match": {
                  "modelName": {
                    "query": "?0",
                    "boost": 5,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "modelName.keyword": {
                    "value": "*?0*",
                    "boost": 3,
                    "case_insensitive": true
                  }
                }
              },
              {
                "match": {
                  "prompt": {
                    "query": "?0",
                    "boost": 2,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "tags.keyword": {
                    "value": "*?0*",
                    "boost": 1.5,
                    "case_insensitive": true
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownType": "ADMIN"
                }
              },
              {
                "term": {
                  "isPublic": true
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<AIModelDocument> searchPaidAdminModelsWithKeyword(String keyword, Pageable pageable);

    /**
     * 유료 사용자 모델 조회 (특정 사용자 소유 + 유료)
     */
    @Query("""
        {
          "bool": {
            "filter": [
              {
                "term": {
                  "ownerId": ?0
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ]
          }
        }
        """)
    Page<AIModelDocument> searchPaidUserModels(Long userId, Pageable pageable);

    /**
     * 유료 사용자 모델에서 키워드 검색 (특정 사용자 소유 + 유료 + 키워드)
     */
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "modelName": {
                    "query": "?0",
                    "boost": 10
                  }
                }
              },
              {
                "match": {
                  "modelName": {
                    "query": "?0",
                    "boost": 5,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "modelName.keyword": {
                    "value": "*?0*",
                    "boost": 3,
                    "case_insensitive": true
                  }
                }
              },
              {
                "match": {
                  "prompt": {
                    "query": "?0",
                    "boost": 2,
                    "fuzziness": "AUTO"
                  }
                }
              },
              {
                "wildcard": {
                  "tags.keyword": {
                    "value": "*?0*",
                    "boost": 1.5,
                    "case_insensitive": true
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "ownerId": ?1
                }
              },
              {
                "exists": {
                  "field": "price"
                }
              },
              {
                "range": {
                  "price": {
                    "gt": 0
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<AIModelDocument> searchPaidUserModelsWithKeyword(String keyword, Long userId, Pageable pageable);
}